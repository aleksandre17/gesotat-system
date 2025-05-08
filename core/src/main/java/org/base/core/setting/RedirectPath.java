package org.base.core.setting;

import org.base.core.anotation.Sign;
import org.base.core.anotation.Web;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

@Configuration
public class RedirectPath implements WebMvcConfigurer, WebMvcRegistrations {  //WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>


    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {
            @Override
            protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
                Class<?> controllerClass = method.getDeclaringClass();
                if (controllerClass.isAnnotationPresent(Sign.class) || controllerClass.isAnnotationPresent(Web.class)) {
                    super.registerHandlerMethod(handler, method, mapping);
                } else {
                    RequestMappingInfo newMapping = RequestMappingInfo
                            .paths("/api/v1")
                            .build()
                            .combine(mapping);
                    super.registerHandlerMethod(handler, method, newMapping);
                }
            }
        };
    }


//    @Bean
//    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
//        return factory -> {
//            if (factory instanceof TomcatServletWebServerFactory) {
//                ((TomcatServletWebServerFactory) factory).addContextCustomizers(context -> {
//                    context.addServletMappingDecoder(request -> {
//                        if (request.getRequestURI().startsWith("/auth/")) {
//                            return "";
//                        }
//                        return "/api/v1";
//                    });
//                });
//            }
//        };
//    }


//    @GetMapping("/auth/**")
//    public void forwardAuth(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        String path = request.getServletPath();
//        request.getRequestDispatcher(path).forward(request, response);
//    }



//    @Bean
//    public ServletRegistrationBean authServlet() {
//        ServletRegistrationBean registration = new ServletRegistrationBean(new DispatcherServlet());
//        registration.addUrlMappings("/auth/*");
//        registration.setLoadOnStartup(1);
//        return registration;
//    }


//    @Bean
//    public TomcatServletWebServerFactory servletContainer() {
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//        tomcat.addContextCustomizers(context -> {
//            context.setPath("");
//        });
//        return tomcat;
//    }


//    @Bean
//    public WebMvcConfigurer webMvcConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void configurePathMatch(PathMatchConfigurer configurer) {
//                configurer.addPathPrefix("/api/v1", c -> !c.isAnnotationPresent(Auth.class));
//            }
//        };
//    }


//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        registry.addResourceHandler("/auth/**")
//                .addResourceLocations("classpath:/public/")
//                .resourceChain(false);
//    }
//
//    @Bean
//    public ServletRegistrationBean authServlet() {
//        ServletRegistrationBean registration = new ServletRegistrationBean(new DispatcherServlet());
//        registration.addUrlMappings("/auth/*");
//        registration.setOrder(1);
//        return registration;
//    }

//    @Bean
//    public ForwardedHeaderFilter forwardedHeaderFilter() {
//        return new ForwardedHeaderFilter();
//    }
//
//    @Bean
//    public WebMvcConfigurer webMvcConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addViewControllers(ViewControllerRegistry registry) {
//                registry.addViewController("/api/v1/auth/**").setViewName("forward:/auth/**");
//            }
//        };
//    }



//    @Bean
//    public ServletRegistrationBean authServlet() {
//        ServletRegistrationBean registration = new ServletRegistrationBean(new DispatcherServlet());
//        registration.addUrlMappings("/auth/*");
//        registration.setLoadOnStartup(1);
//        return registration;
//    }


//    @Bean
//    public WebMvcRegistrations webMvcRegistrations() {
//        return new WebMvcRegistrations() {
//            @Override
//            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
//                return new RequestMappingHandlerMapping() {
//                    @Override
//                    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
//                        if (method.getDeclaringClass().equals(AuthController.class)) {
//                            super.registerHandlerMethod(handler, method,
//                                    RequestMappingInfo.paths("/auth/**")
//                                            .build());
//                        } else {
//                            super.registerHandlerMethod(handler, method, mapping);
//                        }
//                    }
//                };
//            }
//        };
//    }


//    @Override
//    public void customize(ConfigurableServletWebServerFactory factory) {
//        factory.addContextCustomizers(context -> {
//            context.addServletContainerInitializer(new ServletContainerInitializer() {
//                @Override
//                public void onStartup(Set<Class<?>> c, ServletContext ctx) {
//                    Dynamic servlet = ctx.addServlet("authServlet", new DispatcherServlet());
//                    servlet.addMapping("/auth/*");
//                    servlet.setLoadOnStartup(1);
//                }
//            }, null);
//        });
//    }


//    @Override
//    public void configurePathMatch(PathMatchConfigurer configurer) {
//        configurer.addPathPrefix("/api/v1", c ->
//                !c.isAnnotationPresent(RequestMapping.class) ||
//                        !c.getAnnotation(RequestMapping.class).value()[0].startsWith("/auth")
//        );
//    }



//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
//    }
//
//    @Override
//    public void configurePathMatch(PathMatchConfigurer configurer) {
//        configurer.addPathPrefix("/api/v1", c -> !c.getName().contains("AuthController"));
//    }


}
