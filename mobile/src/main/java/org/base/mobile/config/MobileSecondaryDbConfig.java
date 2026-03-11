package org.base.mobile.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Secondary database JPA configuration for mobile module.
 * <p>
 * Uses {@code @ConditionalOnMissingBean} so api module can override if needed.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "org.base.mobile.repository.secondary",
        entityManagerFactoryRef = "secondaryEntityManagerFactory",
        transactionManagerRef = "secondaryTransactionManager"
)
public class MobileSecondaryDbConfig {

    @Bean(name = "secondaryJpaProperties")
    @ConfigurationProperties(prefix = "spring.jpa.secondary")
    @ConditionalOnMissingBean(name = "secondaryJpaProperties")
    public Map<String, String> secondaryJpaProperties() {
        return new HashMap<>();
    }

    @Bean(name = "secondaryEntityManagerFactory")
    @ConditionalOnMissingBean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEntityManagerFactory(
            @Qualifier("secondaryDataSource") DataSource dataSource,
            @Qualifier("secondaryJpaProperties") Map<String, String> jpaProperties) {

        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.base.mobile.entity");

        var adapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(adapter);

        Map<String, Object> props = new HashMap<>(jpaProperties);
        em.setJpaPropertyMap(props);

        return em;
    }

    @Bean(name = "secondaryTransactionManager")
    @ConditionalOnMissingBean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTransactionManager(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}

