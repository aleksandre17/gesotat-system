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

import java.util.*;

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

            // Create a list to store all route information
            List<RouteInfo> routes = new ArrayList<>();

            map.forEach((mapping, method) -> {
                Set<String> patterns = mapping.getPatternValues();
                Set<RequestMethod> httpMethods = mapping.getMethodsCondition().getMethods();

                patterns.forEach(pattern ->
                        httpMethods.forEach(httpMethod ->
                                routes.add(new RouteInfo(
                                        httpMethod,
                                        pattern,
                                        method.getBeanType().getSimpleName(),
                                        method.getMethod().getName()
                                ))
                        )
                );
            });

            // Sort routes by HTTP method and then by pattern
            routes.stream()
                    .sorted(Comparator
                            .comparing((RouteInfo r) -> r.httpMethod != null ? r.httpMethod.name() : "")
                            .thenComparing(r -> r.pattern))
                    .forEach(route ->
                            logger.info("{} {} => {}#{}()",
                                    route.httpMethod,
                                    route.pattern,
                                    route.controllerName,
                                    route.methodName)
                    );

            logger.info("=====================");
        }

        private record RouteInfo(RequestMethod httpMethod, String pattern, String controllerName, String methodName) {}
    }


}

