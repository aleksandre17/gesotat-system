package org.base.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"org.base.api", "org.base.core"}) // Add this line
//@EntityScan(basePackages = {"org.base.core.entity", "org.base.api.entity.mobile", "org.base.api.entity.primary"}) // Add this line
//@EnableJpaRepositories(basePackages = {"org.base.core.repository", "org.base.api.repository.mobile", "org.base.api.repository.primary"}) // Add this line
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }

}
