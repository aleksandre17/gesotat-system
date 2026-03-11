package org.base.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally loads the entire mobile module.
 * Set mobile.enabled=false in application-local.yml to disable.
 */
@Configuration
@ConditionalOnProperty(name = "mobile.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan("org.base.mobile")
public class MobileModuleConfig {
}