package org.base.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Conditionally loads the entire mobile module.
 * Set mobile.enabled=false in application-local.yml to disable.
 * HealthController is excluded because api module provides its own.
 */
@Configuration
@ConditionalOnProperty(name = "mobile.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(
        basePackages = "org.base.mobile",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = org.base.mobile.controller.HealthController.class
        )
)
public class MobileModuleConfig {
}