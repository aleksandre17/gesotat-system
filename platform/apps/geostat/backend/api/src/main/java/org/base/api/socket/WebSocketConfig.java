package org.base.api.socket;

import org.base.core.service.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Objects;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://192.168.1.199:5175", "http://localhost:5173", "http://192.168.2.87:8085", "http://localhost:8085")
                .withSockJS();
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://192.168.1.199:5175", "http://localhost:5173", "http://192.168.2.87:8085", "http://localhost:8085");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String taskId = accessor.getFirstNativeHeader("X-Task-ID");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwt = authHeader.substring(7);
                        if (taskId == null || taskId.isEmpty()) {
                            throw new IllegalArgumentException("Missing X-Task-ID");
                        }
                        try {
                            String username = jwtTokenUtil.extractUsername(jwt);
                            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                            if (jwtTokenUtil.isTokenValid(jwt, userDetails)) {
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                        taskId, null, userDetails.getAuthorities());
                                accessor.setUser(auth);
                                accessor.setSessionAttributes(new HashMap<>());
                                Objects.requireNonNull(accessor.getSessionAttributes()).put("taskId", taskId);
                            } else {
                                throw new IllegalArgumentException("Invalid JWT token");
                            }
                        } catch (Exception e) {
                            log.warn("WebSocket JWT authentication failed: {}", e.getClass().getSimpleName());
                            throw new IllegalArgumentException("WebSocket authentication failed");
                        }
                    } else {
                        throw new IllegalArgumentException("Missing or invalid Authorization header");
                    }
                }
                return message;
            }
        });
    }
}
