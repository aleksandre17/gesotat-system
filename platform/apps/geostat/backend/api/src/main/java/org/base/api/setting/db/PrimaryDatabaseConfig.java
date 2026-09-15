package org.base.api.setting.db;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = "org.base.core.repository",
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager"
)
public class PrimaryDatabaseConfig {

    @Bean(name = "primaryJpaProperties")
    @ConfigurationProperties(prefix = "spring.jpa.primary")
    public Map<String, String> primaryJpaProperties() {
        return new HashMap<>();
    }

    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource,
            @Qualifier("primaryJpaProperties") Map<String, String> jpaProperties) {

        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("org.base.core.entity");

        var adapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(adapter);

        Map<String, Object> props = new HashMap<>(jpaProperties);
        em.setJpaPropertyMap(props);

        return em;
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public JpaTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory") LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(Objects.requireNonNull(emf.getObject()));
    }
}

