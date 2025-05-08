package org.base.core.setting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;

@Configuration
public class RouteLoggingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RouteLoggingConfig.class);

    @Bean
    public RouteLoggingBean routeLoggingBean(RequestMappingHandlerMapping handlerMapping) {
        return new RouteLoggingBean(handlerMapping);
    }


    @Component
    public static class RouteLoggingBean {
        public RouteLoggingBean(RequestMappingHandlerMapping handlerMapping) {
            Map<RequestMappingInfo, HandlerMethod> map = handlerMapping.getHandlerMethods();

            logger.info("=== Registered Routes ===");
            map.forEach((mapping, method) -> {
                // Get the actual paths after all transformations
                Set<String> patterns = mapping.getPatternValues();
                Set<RequestMethod> httpMethods = mapping.getMethodsCondition().getMethods();

                patterns.forEach(pattern ->
                        httpMethods.forEach(httpMethod ->
                                logger.info("{} {} => {}#{}()",
                                        httpMethod,
                                        pattern,
                                        method.getBeanType().getSimpleName(),
                                        method.getMethod().getName())
                        )
                );
            });
            logger.info("=====================");
        }
    }

}

