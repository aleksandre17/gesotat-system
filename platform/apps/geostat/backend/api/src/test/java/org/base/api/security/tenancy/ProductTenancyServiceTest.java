package org.base.api.security.tenancy;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Governed assignment: idempotent, explicit and audited on every effective change. */
class ProductTenancyServiceTest {
    private final ProductTenancyRepository products = mock(ProductTenancyRepository.class);
    private final ProductTenancyService service = new ProductTenancyService(products, mock(PlatformTransactionManager.class));

    private void product(String tenantKey) {
        when(products.lockByProductCode("PRODUCT_A")).thenReturn(Optional.of(new ProductTenancy(1, "PRODUCT_A", tenantKey)));
    }

    @Test
    void firstAssignmentRecordsTheOwnerAndTheHistory() {
        product(null);
        var receipt = service.assign("PRODUCT_A", "tenant-a", false, null, "operator");
        assertTrue(receipt.changed());
        assertFalse(receipt.transfer());
        assertEquals("tenant-a", receipt.tenantKey());
        verify(products).assign(1L, null, "tenant-a", false, null, "operator");
    }

    @Test
    void reassigningTheSameTenantChangesAndRecordsNothing() {
        product("tenant-a");
        var receipt = service.assign("PRODUCT_A", "tenant-a", false, null, "operator");
        assertFalse(receipt.changed());
        verify(products, never()).assign(anyLong(), any(), anyString(), anyBoolean(), any(), anyString());
    }

    @Test
    void movingAnAssignedProductNeedsAnExplicitTransfer() {
        product("tenant-a");
        assertThrows(IllegalStateException.class, () -> service.assign("PRODUCT_A", "tenant-b", false, "merger", "operator"));
    }

    @Test
    void aTransferNeedsARecordedReason() {
        product("tenant-a");
        assertThrows(IllegalArgumentException.class, () -> service.assign("PRODUCT_A", "tenant-b", true, "  ", "operator"));
    }

    @Test
    void anAuditedTransferIsAccepted() {
        product("tenant-a");
        var receipt = service.assign("PRODUCT_A", "tenant-b", true, "portfolio handover GEO-42", "operator");
        assertTrue(receipt.transfer());
        assertEquals("tenant-a", receipt.previousTenantKey());
        verify(products).assign(1L, "tenant-a", "tenant-b", true, "portfolio handover GEO-42", "operator");
    }

    @Test
    void anUnknownProductAndABlankTenantAreRefused() {
        when(products.lockByProductCode("MISSING")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.assign("MISSING", "tenant-a", false, null, "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.assign("PRODUCT_A", "   ", false, null, "operator"));
        assertThrows(IllegalArgumentException.class, () -> service.assign("PRODUCT_A", "tenant-a", false, null, " "));
    }
}
