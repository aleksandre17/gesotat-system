package org.base.api.controller;

import org.base.api.security.tenancy.ProductTenancy;
import org.base.api.security.tenancy.ProductTenancyService;
import org.base.api.security.tenancy.TenantAccessDeniedException;
import org.base.api.security.tenancy.TenantAccessOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract of the governed tenant assignment surface. */
class PlatformProductTenancyControllerTest {
    private static final String PATH = "/platform/products/PRODUCT_A/tenant";
    private final ProductTenancyService tenancy = mock(ProductTenancyService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new PlatformProductTenancyController(tenancy))
                .setControllerAdvice(new TenantAccessExceptionHandler()).build();
    }

    @Test
    void assignmentIsNeverCachedAndReportsWhetherItChangedAnything() throws Exception {
        when(tenancy.assign(eq("PRODUCT_A"), eq("tenant-a"), anyBoolean(), any(), anyString()))
                .thenReturn(new ProductTenancyService.AssignmentReceipt("PRODUCT_A", "tenant-a", null, true, false));
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantKey\":\"tenant-a\",\"transfer\":false}")
                        .principal(new UsernamePasswordAuthenticationToken("operator", "n/a")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.tenantKey").value("tenant-a"));
    }

    @Test
    void anUnauditedReassignmentIsAConflict() throws Exception {
        when(tenancy.assign(anyString(), anyString(), anyBoolean(), any(), anyString()))
                .thenThrow(new IllegalStateException("Data product is already assigned to another tenant; an explicit, audited transfer is required"));
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantKey\":\"tenant-b\",\"transfer\":false}").principal(new UsernamePasswordAuthenticationToken("operator", "n/a")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("product-tenant-assignment-conflict"));
    }

    @Test
    void anUnknownProductIsProblem404() throws Exception {
        when(tenancy.read("PRODUCT_A")).thenThrow(new NoSuchElementException("Data product not found: PRODUCT_A"));
        mvc.perform(get(PATH)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("data-product-not-found"));
    }

    @Test
    void anUnassignedProductIsReportedAsUnassigned() throws Exception {
        when(tenancy.read("PRODUCT_A")).thenReturn(new ProductTenancy(1, "PRODUCT_A", null));
        mvc.perform(get(PATH)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.assigned").value(false));
    }

    @Test
    void aTenancyDenialIsProblem403WithoutObjectAttributesAndIsNeverCached() throws Exception {
        when(tenancy.read("PRODUCT_A")).thenThrow(new TenantAccessDeniedException(TenantAccessOutcome.DENIED_TENANT_MISMATCH));
        mvc.perform(get(PATH)).andExpect(status().isForbidden())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("tenant-access-denied"))
                .andExpect(jsonPath("$.detail").value(TenantAccessDeniedException.DETAIL));
    }
}
