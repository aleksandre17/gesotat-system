package org.base.api.security.tenancy;

import org.base.api.security.tenancy.scope.AsyncOperationScopeResolver;
import org.base.api.security.tenancy.scope.ContractCodeScopeResolver;
import org.base.api.security.tenancy.scope.DataPlaneObjectScopeResolver;
import org.base.api.security.tenancy.scope.IngestionContractScopeResolver;
import org.base.api.security.tenancy.scope.ProductScopeResolver;
import org.base.api.security.tenancy.scope.TenantScopeResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Coverage is a declared, tested property rather than a list of URL patterns, and omission must be
 * impossible: a controller that declares no tenancy scope fails the build, and a scoped handler whose
 * route carries no resolvable identity and no documented exemption fails the build too.
 */
class TenantScopeCoverageTest {
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([A-Za-z0-9_]+)(?::[^}]*)?}");

    /** The identities the real resolvers read. The test can never claim coverage they do not give. */
    private static Set<String> resolvableIdentities() {
        ProductTenancyRepository products = mock(ProductTenancyRepository.class);
        List<TenantScopeResolver> resolvers = List.of(new ContractCodeScopeResolver(products),
                new ProductScopeResolver(products), new DataPlaneObjectScopeResolver(products),
                new IngestionContractScopeResolver(products), new AsyncOperationScopeResolver(products));
        Set<String> identities = new TreeSet<>();
        resolvers.forEach(resolver -> identities.addAll(resolver.identities()));
        return identities;
    }

    private static List<Class<?>> governedControllers() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(TenantScopeDeclarations.GOVERNED_CONTROLLER_PACKAGE)) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            // Exception advice carries no route and serves no object.
            if (AnnotatedElementUtils.hasAnnotation(type, RestControllerAdvice.class)) continue;
            controllers.add(type);
        }
        controllers.sort(java.util.Comparator.comparing(Class::getName));
        return controllers;
    }

    private static List<Method> handlers(Class<?> controller) {
        List<Method> handlers = new ArrayList<>();
        for (Method method : controller.getDeclaredMethods()) {
            if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) handlers.add(method);
        }
        handlers.sort(java.util.Comparator.comparing(Method::getName));
        return handlers;
    }

    /** Every identity the route can carry: path template variables plus declared request parameters. */
    private static Set<String> routeIdentities(Class<?> controller, Method handler) {
        Set<String> identities = new LinkedHashSet<>();
        for (String template : templates(controller, handler)) {
            Matcher matcher = PATH_VARIABLE.matcher(template);
            while (matcher.find()) identities.add(matcher.group(1));
        }
        for (Parameter parameter : handler.getParameters()) {
            PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
            if (pathVariable != null) identities.add(named(pathVariable.name(), pathVariable.value(), parameter));
            RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
            if (requestParam != null) identities.add(named(requestParam.name(), requestParam.value(), parameter));
        }
        return identities;
    }

    private static String named(String name, String value, Parameter parameter) {
        if (name != null && !name.isBlank()) return name;
        if (value != null && !value.isBlank()) return value;
        return parameter.getName();
    }

    private static List<String> templates(Class<?> controller, Method handler) {
        List<String> templates = new ArrayList<>();
        RequestMapping type = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        RequestMapping method = AnnotatedElementUtils.findMergedAnnotation(handler, RequestMapping.class);
        String[] typePaths = type == null || type.path().length == 0 ? new String[]{""} : type.path();
        String[] methodPaths = method == null || method.path().length == 0 ? new String[]{""} : method.path();
        for (String typePath : typePaths) for (String methodPath : methodPaths) templates.add(typePath + "/" + methodPath);
        return templates;
    }

    @Test
    void everyGovernedControllerDeclaresExactlyOneTenancyScope() throws Exception {
        List<String> undeclared = new ArrayList<>();
        List<String> blankReason = new ArrayList<>();
        for (Class<?> controller : governedControllers()) {
            TenantScopeClassification classification = TenantScopeDeclarations.classify(controller);
            if (classification == TenantScopeClassification.UNDECLARED) {
                undeclared.add(controller.getName());
                continue;
            }
            TenantNeutral neutral = AnnotatedElementUtils.findMergedAnnotation(controller, TenantNeutral.class);
            if (neutral != null && (neutral.reason() == null || neutral.reason().isBlank())) blankReason.add(controller.getName());
        }
        assertTrue(undeclared.isEmpty(),
                "controller must declare @TenantScoped or @TenantNeutral: " + String.join(", ", undeclared));
        assertTrue(blankReason.isEmpty(), "@TenantNeutral needs an honest reason: " + String.join(", ", blankReason));
    }

    @Test
    void everyScopedHandlerCarriesAResolvableIdentityOrADocumentedExemption() throws Exception {
        Set<String> resolvable = resolvableIdentities();
        List<String> unscoped = new ArrayList<>();
        List<String> blankReason = new ArrayList<>();
        for (Class<?> controller : governedControllers()) {
            if (TenantScopeDeclarations.classify(controller) != TenantScopeClassification.SCOPED) continue;
            for (Method handler : handlers(controller)) {
                TenantScopeExemption exemption = AnnotatedElementUtils.findMergedAnnotation(handler, TenantScopeExemption.class);
                if (exemption != null) {
                    if (exemption.reason() == null || exemption.reason().isBlank())
                        blankReason.add(controller.getSimpleName() + "#" + handler.getName());
                    continue;
                }
                Set<String> identities = routeIdentities(controller, handler);
                identities.retainAll(resolvable);
                if (identities.isEmpty()) unscoped.add(controller.getSimpleName() + "#" + handler.getName());
            }
        }
        assertTrue(unscoped.isEmpty(), "scoped handler carries no resolvable identity and no @TenantScopeExemption: "
                + String.join(", ", unscoped) + " (resolvable identities: " + resolvable + ")");
        assertTrue(blankReason.isEmpty(), "@TenantScopeExemption needs a reason: " + String.join(", ", blankReason));
    }

    @Test
    void theGovernedSurfaceIsNotSilentlyShrinkingToNeutral() throws Exception {
        long scoped = governedControllers().stream()
                .filter(type -> TenantScopeDeclarations.classify(type) == TenantScopeClassification.SCOPED).count();
        // A regression that re-declared governed controllers as neutral would pass the first test.
        assertTrue(scoped >= 19, "the governed product-scoped surface shrank unexpectedly: " + scoped);
    }

    @Test
    void everyLegacyNeutralSurfaceIsRecordedWithItsReason() throws Exception {
        List<String> legacy = new ArrayList<>();
        for (Class<?> controller : governedControllers()) {
            TenantNeutral neutral = AnnotatedElementUtils.findMergedAnnotation(controller, TenantNeutral.class);
            if (neutral != null && neutral.legacy()) {
                assertTrue(neutral.reason().contains("LEGACY"),
                        "a legacy neutral surface must say so in its reason: " + controller.getSimpleName());
                legacy.add(controller.getSimpleName());
            }
        }
        // Recorded, not endorsed: the ADR lists exactly these surfaces as open gaps.
        assertFalse(legacy.isEmpty(), "the legacy classification exists and must stay visible");
    }
}
