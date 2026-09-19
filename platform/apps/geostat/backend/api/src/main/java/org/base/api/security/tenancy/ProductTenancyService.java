package org.base.api.security.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;

/**
 * Governed assignment of a data product to a tenant. Idempotent: re-assigning the tenant a product
 * already has changes nothing and records nothing. Moving a product to a different tenant is a
 * transfer and is refused unless it is requested explicitly and carries a reason; every effective
 * assignment appends one immutable history row.
 */
@Service
public class ProductTenancyService {
    private static final Logger log = LoggerFactory.getLogger(ProductTenancyService.class);
    private static final int MAX_TENANT_KEY = 160;
    private static final int MAX_REASON = 1000;

    private final ProductTenancyRepository products;
    private final TransactionTemplate transaction;

    /** Outcome of an assignment: the resulting owner and whether this call changed it. */
    public record AssignmentReceipt(String productCode, String tenantKey, String previousTenantKey, boolean changed, boolean transfer) {}

    public ProductTenancyService(ProductTenancyRepository products,
                                 @Qualifier("primaryJdbcTransactionManager") PlatformTransactionManager transactionManager) {
        this.products = products;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public ProductTenancy read(String productCode) {
        return products.byProductCode(productCode)
                .orElseThrow(() -> new NoSuchElementException("Data product not found: " + productCode));
    }

    public AssignmentReceipt assign(String productCode, String tenantKey, boolean transfer, String reason, String actor) {
        String product = require(productCode, "productCode", MAX_TENANT_KEY);
        String tenant = require(tenantKey, "tenantKey", MAX_TENANT_KEY);
        String who = require(actor, "actor", 255);
        if (reason != null && reason.length() > MAX_REASON) throw new IllegalArgumentException("reason exceeds " + MAX_REASON + " characters");
        return transaction.execute(status -> {
            ProductTenancy current = products.lockByProductCode(product)
                    .orElseThrow(() -> new NoSuchElementException("Data product not found: " + product));
            if (tenant.equals(current.tenantKey()))
                return new AssignmentReceipt(current.productCode(), tenant, tenant, false, false);
            boolean isTransfer = current.assigned();
            if (isTransfer && !transfer)
                throw new IllegalStateException("Data product is already assigned to another tenant; an explicit, audited transfer is required");
            if (isTransfer && (reason == null || reason.isBlank()))
                throw new IllegalArgumentException("A tenant transfer requires a recorded reason");
            products.assign(current.productId(), current.tenantKey(), tenant, isTransfer, reason, who);
            log.info("tenancy.assignment product={} transfer={} assignedBy={}", current.productCode(), isTransfer, who);
            return new AssignmentReceipt(current.productCode(), tenant, current.tenantKey(), true, isTransfer);
        });
    }

    private static String require(String value, String name, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException(name + " exceeds " + max + " characters");
        return trimmed;
    }
}
