package org.base.api.service.catalog;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCatalogServiceTest {

    @Test
    void recognizesOnlyPackagesWithEveryMandatoryMetadataTable() {
        assertTrue(AccessCatalogService.isManagedPackage(Set.of(
                "__gs_package", "__gs_dataset", "__gs_chart", "__gs_chart_filter", "auto_main"
        )));
        assertFalse(AccessCatalogService.isManagedPackage(Set.of(
                "__gs_package", "__gs_dataset", "auto_main"
        )));
    }
}
