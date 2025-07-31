package org.base.api.setting.db;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableJpaRepositories(
        basePackages = {"org.base.api.repository.primary", "org.base.core.repository"},
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryDatabaseConfig {

    @Bean(name = "primaryJpaProperties")
    @ConfigurationProperties(prefix = "spring.jpa.primary")
    public Map<String, Object> primaryJpaProperties() {
        return new HashMap<>();
    }

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") DataSource dataSource,
            @Qualifier("primaryJpaProperties") Map<String, Object> primaryJpaProperties) {
        return builder
                .dataSource(dataSource)
                .packages("org.base.api.entity.primary", "org.base.core.entity")
                .persistenceUnit("primary")
                .properties(primaryJpaProperties)
                .build();
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public JpaTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
