package org.base.core;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

//@SpringBootApplication
//@Configuration
//@ComponentScan("org.base.core")

@Configuration
@ComponentScan(basePackages = "org.base.core")
@EnableJpaRepositories(basePackages = "org.base.core.repository")
@EntityScan(basePackages = "org.base.core.entity")
public class CoreApplication {

//    public static void main(String[] args) {
//        SpringApplication.run(CoreApplication.class, args);
//    }

}
