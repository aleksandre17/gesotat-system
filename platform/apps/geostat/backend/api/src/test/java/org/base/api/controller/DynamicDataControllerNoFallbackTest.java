package org.base.api.controller;

import org.base.api.service.dynamic.DynamicChartService;
import org.base.api.service.dynamic.DynamicTableService;
import org.base.api.service.platform.ApprovedContractResolver;
import org.base.api.service.platform.CanonicalPageDataService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DynamicDataControllerNoFallbackTest {
    @Test void disabledLegacyFallbackFailsClosedWhenNoApprovedContractExists() {
        var resolver = mock(ApprovedContractResolver.class);
        when(resolver.resolve()).thenReturn(null);
        var legacyTable = mock(DynamicTableService.class);
        var controller = new DynamicDataController(legacyTable, mock(DynamicChartService.class),
                mock(CanonicalPageDataService.class), resolver, false);
        assertThrows(IllegalStateException.class, () -> controller.data(99L, 1, 100, null));
        verifyNoInteractions(legacyTable);
    }
}
