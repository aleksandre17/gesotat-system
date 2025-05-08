package org.base.core.setting.web;

import lombok.AllArgsConstructor;
import org.base.core.request.interceptor.HandlerMethodInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AllArgsConstructor
@Configuration
public class WebMvc implements WebMvcConfigurer {

    private HandlerMethodInterceptor handlerMethodInterceptor;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //registry.addViewController("/error").setViewName("error/general");
        registry.addViewController("/login*").setViewName("login");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/.well-known/**")
                .addResourceLocations("classpath:/static/.well-known/", "classpath:/public/.well-known/");

        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);

        registry.addResourceHandler("/*.css", "/*.js", "/*.ico", "/*.png", "/*.jpg")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);

    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(handlerMethodInterceptor).addPathPatterns("/**").excludePathPatterns("/static/**", "/*.css", "/*.js", "/*.ico");
    }

}

