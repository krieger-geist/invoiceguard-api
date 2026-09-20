package com.invoiceguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * InvoiceGuard API — secure, multi-tenant invoice-risk analysis platform.
 *
 * <p>Bootstraps the modular monolith. JPA auditing is enabled here so that
 * {@code @CreatedDate}/{@code @LastModifiedDate}/{@code @CreatedBy} fields on
 * {@link com.invoiceguard.common.entity.BaseEntity} are populated automatically.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class InvoiceGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceGuardApplication.class, args);
    }
}
