package org.base.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "platform.schema-migration.enabled=false",
        "spring.profiles.active=test",
        "spring.config.import=optional:classpath:application-test.yml",
        "spring.task.scheduling.enabled=false",
        "spring.jpa.primary.hibernate.dialect=org.hibernate.dialect.SQLServerDialect",
        "spring.datasource.primary.jdbc-url=jdbc:sqlserver://localhost:65535;databaseName=geostat-control;encrypt=true;trustServerCertificate=true",
        "spring.datasource.primary.username=test",
        "spring.datasource.primary.password=test",
        "spring.datasource.secondary.jdbc-url=jdbc:sqlserver://localhost:65535;databaseName=geostat-secondary;encrypt=true;trustServerCertificate=true",
        "spring.datasource.secondary.username=test",
        "spring.datasource.secondary.password=test",
        "spring.datasource.data-plane.jdbc-url=jdbc:sqlserver://localhost:65535;databaseName=geostat-data;encrypt=true;trustServerCertificate=true",
        "spring.datasource.data-plane.username=test",
        "spring.datasource.data-plane.password=test",
        "spring.datasource.archive-plane.jdbc-url=jdbc:sqlserver://localhost:65535;databaseName=geostat-archive;encrypt=true;trustServerCertificate=true",
        "spring.datasource.archive-plane.username=test",
        "spring.datasource.archive-plane.password=test",
        "jwt.secret=test-only-context-secret-test-only-context-secret-test-only-context-secret",
        "platform.rate-limit.redis.enabled=false",
        "platform.provider.discovery.enabled=false",
        "platform.serving-cache.enabled=false",
        "platform.outbox.enabled=false"
})
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
