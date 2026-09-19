package org.base.api.security.tenancy;

import org.base.api.security.tenancy.scope.AsyncOperationScopeResolver;
import org.base.api.security.tenancy.scope.ContractCodeScopeResolver;
import org.base.api.security.tenancy.scope.DataPlaneObjectScopeResolver;
import org.base.api.security.tenancy.scope.IngestionContractScopeResolver;
import org.base.api.security.tenancy.scope.ProductScopeResolver;
import org.base.api.security.tenancy.scope.TenantScopeResolver;
import org.base.api.service.platform.ApprovedContractResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The declared coverage model at runtime: the controller's annotation decides, the resolvers name the
 * product, and a scoped route nobody can scope is denied.
 */
class TenantScopedAccessInterceptorTest {
    private static final ProductTenancy OWNED = new ProductTenancy(1, "PRODUCT_A", "tenant-a");

    private final ProductTenancyRepository products = mock(ProductTenancyRepository.class);
    private final TenantAccessGuard guard = mock(TenantAccessGuard.class);
    private final ApprovedContractResolver defaults = mock(ApprovedContractResolver.class);
    private final List<TenantScopeResolver> resolvers = List.of(new ContractCodeScopeResolver(products),
            new ProductScopeResolver(products), new DataPlaneObjectScopeResolver(products),
            new IngestionContractScopeResolver(products), new AsyncOperationScopeResolver(products));
    private final TenantScopedAccessInterceptor interceptor = new TenantScopedAccessInterceptor(guard, defaults, resolvers);

    // --- test controllers standing in for the declared classifications -------------------------

    @TenantScoped
    @RestController
    @RequestMapping("/scoped")
    static class ScopedController {
        @GetMapping("/{contractCode}") public String byContract() { return "ok"; }
        @GetMapping("/nothing") public String withoutIdentity() { return "ok"; }
        @GetMapping("/default")
        @TenantScopeExemption(value = TenantScopeExemption.Kind.DEFAULT_CONTRACT, reason = "falls back to the approved default contract")
        public String byDefaultContract() { return "ok"; }
        @GetMapping("/body")
        @TenantScopeExemption(value = TenantScopeExemption.Kind.SERVICE_ENFORCED, reason = "identity is in the body")
        public String byService() { return "ok"; }
    }

    @TenantNeutral(reason = "carries no product identity")
    @RestController
    static class NeutralController {
        @GetMapping("/neutral") public String anything() { return "ok"; }
    }

    @RestController
    static class UndeclaredController {
        @GetMapping("/undeclared") public String anything() { return "ok"; }
    }

    private boolean handle(Class<?> controller, String methodName, String uri,
                           Map<String, String> pathVariables, Map<String, String> parameters) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);
        parameters.forEach(request::setParameter);
        Method method = java.util.Arrays.stream(controller.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        Object handler = new HandlerMethod(mock(controller), method);
        return interceptor.preHandle(request, new MockHttpServletResponse(), handler);
    }

    @Test
    void aContractCodeOnTheRouteNamesTheProduct() {
        when(products.byContractCode("C")).thenReturn(Optional.of(OWNED));
        assertTrue(handle(ScopedController.class, "byContract", "/api/v1/scoped/C", Map.of("contractCode", "C"), Map.of()));
        verify(guard).require("contractCode", "C", OWNED);
    }

    @Test
    void aContractCodeInAQueryParameterNamesTheProductToo() {
        when(products.byContractCode("C")).thenReturn(Optional.of(OWNED));
        handle(ScopedController.class, "withoutIdentity", "/api/v1/scoped/nothing", Map.of(), Map.of("contractCode", "C"));
        verify(guard).require("contractCode", "C", OWNED);
    }

    @Test
    void everyResolvableIdentityFamilyReachesTheGuard() {
        UUID session = UUID.randomUUID();
        when(products.byProductId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byProductCode(anyString())).thenReturn(Optional.of(OWNED));
        when(products.bySnapshotId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byManifestId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byPackageRunId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byDatasetLoadId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byIngestBatchId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byContractSourceId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byIngestionContractId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byAsyncOperationId(anyLong())).thenReturn(Optional.of(OWNED));
        when(products.byUploadSessionId(any())).thenReturn(Optional.of(OWNED));

        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("productId", "4"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("snapshotId", "7"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("manifestId", "9"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("runId", "5"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("datasetLoadId", "11"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("batchId", "13"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of(), Map.of("contractSourceId", "17"));
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("contractId", "19"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("operationId", "23"), Map.of());
        handle(ScopedController.class, "withoutIdentity", "/x", Map.of("uploadSessionId", session.toString()), Map.of());

        verify(guard).require("productId", 4L, OWNED);
        verify(guard).require("snapshotId", 7L, OWNED);
        verify(guard).require("manifestId", 9L, OWNED);
        verify(guard).require("runId", 5L, OWNED);
        verify(guard).require("datasetLoadId", 11L, OWNED);
        verify(guard).require("batchId", 13L, OWNED);
        verify(guard).require("contractSourceId", 17L, OWNED);
        verify(guard).require("contractId", 19L, OWNED);
        verify(guard).require("operationId", 23L, OWNED);
        verify(guard).require("uploadSessionId", session, OWNED);
    }

    @Test
    void aScopedRouteWithNoResolvableIdentityAndNoExemptionIsDenied() {
        TenantAccessDeniedException denial = assertThrows(TenantAccessDeniedException.class,
                () -> handle(ScopedController.class, "withoutIdentity", "/api/v1/scoped/nothing", Map.of(), Map.of()));
        assertTrue(denial.getMessage().equals(TenantAccessDeniedException.DETAIL));
        verify(guard, never()).require(anyString(), any(), any());
    }

    @Test
    void aDefaultContractExemptionResolvesAndEnforcesThatContract() {
        when(defaults.resolve()).thenReturn("DEFAULT_CONTRACT");
        when(products.byContractCode("DEFAULT_CONTRACT")).thenReturn(Optional.of(OWNED));
        assertTrue(handle(ScopedController.class, "byDefaultContract", "/api/v1/scoped/default", Map.of(), Map.of()));
        verify(guard).require("defaultContract", "DEFAULT_CONTRACT", OWNED);
    }

    @Test
    void aDefaultContractExemptionWithoutAnApprovedContractIsStillEnforced() {
        when(defaults.resolve()).thenReturn(null);
        assertTrue(handle(ScopedController.class, "byDefaultContract", "/api/v1/scoped/default", Map.of(), Map.of()));
        // No product to serve: the guard decides on a null product, which the policy denies.
        verify(guard).require("defaultContract", null, null);
    }

    @Test
    void aServiceEnforcedExemptionPassesTheInterceptorAndIsCheckedInTheService() {
        assertTrue(handle(ScopedController.class, "byService", "/api/v1/scoped/body", Map.of(), Map.of()));
        verify(guard, never()).require(anyString(), any(), any());
    }

    @Test
    void aNeutralControllerIsNeverScoped() {
        assertTrue(handle(NeutralController.class, "anything", "/api/v1/neutral", Map.of("contractCode", "C"), Map.of()));
        verify(guard, never()).require(anyString(), any(), any());
    }

    @Test
    void anUndeclaredControllerOutsideTheGovernedPackagePasses() {
        // This test's own controllers live outside org.base.api.controller, which is what makes the
        // core/legacy surfaces that are not part of the governed API keep working.
        assertDoesNotThrow(() -> handle(UndeclaredController.class, "anything", "/api/v1/undeclared", Map.of(), Map.of()));
    }

    @Test
    void aNonHandlerRequestIsNotScoped() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/static/thing");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verify(guard, never()).require(anyString(), any(), any());
    }

    @Test
    void theEnforcementPointRefusesDuplicateResolvers() {
        assertThrows(IllegalStateException.class, () -> new TenantScopedAccessInterceptor(guard, defaults,
                List.of(new ContractCodeScopeResolver(products), new ContractCodeScopeResolver(products))));
        assertTrue(HttpStatus.FORBIDDEN.is4xxClientError());
    }
}
