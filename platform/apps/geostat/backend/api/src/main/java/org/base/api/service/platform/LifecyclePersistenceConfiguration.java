package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/** Runtime wiring: production applications use Control Plane persistence by default. */
@Configuration
public class LifecyclePersistenceConfiguration {
    @Bean
    LifecycleStateStore lifecycleStateStore(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        return new JdbcLifecycleStateStore(controlPlane);
    }

    @Bean
    DataFamilyLifecycleOrchestrator dataFamilyLifecycleOrchestrator(LifecycleStateStore store) {
        return new DataFamilyLifecycleOrchestrator(store);
    }

    @Bean
    ContractLifecycleStateStore contractLifecycleStateStore(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        return new JdbcContractLifecycleStateStore(controlPlane);
    }

    @Bean
    ContractLifecycleOrchestrator contractLifecycleOrchestrator(ContractLifecycleStateStore store) {
        return new ContractLifecycleOrchestrator(store);
    }
}
