package org.base.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Standalone entry point for the Mobile Statistics microservice.
 * <p>
 * Scans only {@code org.base.mobile} — core module provides utility classes
 * (QueryBuilder, ApiException) but no Spring beans are needed from it.
 * <p>
 * Can run independently on its own port, or be embedded as a library
 * within the api module when not started standalone.
 */
@SpringBootApplication(
        scanBasePackages = "org.base.mobile",
        exclude = {
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
        }
)
public class MobileApplication {

    public static void main(String[] args) {
        SpringApplication.run(MobileApplication.class, args);
    }
}

