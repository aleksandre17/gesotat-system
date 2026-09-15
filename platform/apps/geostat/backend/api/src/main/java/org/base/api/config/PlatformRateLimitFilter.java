package org.base.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Bounded single-node admission guard; replace the store with a distributed implementation for replicas. */
@Component
public final class PlatformRateLimitFilter extends OncePerRequestFilter {
    private final int limit;
    private final long windowMs;
    private final int maxKeys;
    private final RateLimitStore store;
    private final Counter accepted;
    private final Counter rejected;

    /** Test/embedded constructor retaining the provider-neutral boundary. */
    public PlatformRateLimitFilter(
            @Value("${platform.rate-limit.requests-per-window:120}") int limit,
            @Value("${platform.rate-limit.window-ms:60000}") long windowMs,
            @Value("${platform.rate-limit.max-client-keys:10000}") int maxKeys,
            RateLimitStore store) {
        this(limit, windowMs, maxKeys, store, new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
    }

    @Autowired
    public PlatformRateLimitFilter(
            @Value("${platform.rate-limit.requests-per-window:120}") int limit,
            @Value("${platform.rate-limit.window-ms:60000}") long windowMs,
            @Value("${platform.rate-limit.max-client-keys:10000}") int maxKeys,
            RateLimitStore store,
            MeterRegistry registry) {
        if (limit < 1 || windowMs < 1000 || maxKeys < 100) throw new IllegalArgumentException("Invalid platform rate-limit configuration");
        this.limit = limit; this.windowMs = windowMs; this.maxKeys = maxKeys; this.store = store;
        this.accepted = Counter.builder("geostat.rate_limit.decisions").tag("outcome", "accepted").description("Accepted platform requests").register(registry);
        this.rejected = Counter.builder("geostat.rate_limit.decisions").tag("outcome", "rejected").description("Rejected platform requests").register(registry);
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String p=request.getRequestURI();
        return !(p.startsWith("/platform/") || p.startsWith("/api/v1/platform/"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        String key=clientKey(request); long now=System.currentTimeMillis();
        RateLimitStore.Window w=store.acquire(key,now,windowMs,maxKeys);
        int used=w.used(); long reset=Math.max(1,(w.startedAt()+windowMs-now+999)/1000); int remaining=Math.max(0,limit-used);
        response.setHeader("RateLimit-Limit",String.valueOf(limit)); response.setHeader("RateLimit-Remaining",String.valueOf(remaining)); response.setHeader("RateLimit-Reset",String.valueOf(reset)); response.setHeader("RateLimit-Policy",limit+";w="+(windowMs/1000));
        if(used>limit){rejected.increment();String correlation=correlationId(request,response);response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());response.setHeader("Retry-After",String.valueOf(reset));response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");response.setContentType("application/problem+json");response.getWriter().write("{\"type\":\"https://api.geostat.ge/problems/rate-limit-exceeded\",\"title\":\"Rate limit exceeded\",\"status\":429,\"detail\":\"Request quota exceeded\",\"correlationId\":\""+correlation+"\"}");return;}
        accepted.increment();
        chain.doFilter(request,response);
    }
    private static String clientKey(HttpServletRequest r){String user=r.getRemoteUser(); if(user!=null&&!user.isBlank())return "u:"+user; return "ip:"+String.valueOf(r.getRemoteAddr());}
    private static String correlationId(HttpServletRequest request,HttpServletResponse response){String value=request.getHeader("X-Correlation-Id");if(value==null||!value.matches("[A-Za-z0-9._:-]{1,128}"))value=java.util.UUID.randomUUID().toString();response.setHeader("X-Correlation-Id",value);return value;}
}
