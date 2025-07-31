package org.base.core.setting;

import org.base.core.anotation.FolderPrefix;
import org.base.core.anotation.Sign;
import org.base.core.anotation.Web;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
    private static final String API_PREFIX = "/api/v1";
    private static final String BASE_PACKAGE = "org.base.api"; // <-- change if needed


    @Override
    protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
        if (mapping == null) {
            super.registerHandlerMethod(handler, method, null);
            return;
        }

        Class<?> controllerClass = method.getDeclaringClass();
        String folderPath;

        // Extract folder path from @FolderPrefix or fallback to package name
        if (AnnotatedElementUtils.hasAnnotation(controllerClass, FolderPrefix.class)) {
            FolderPrefix folderPrefix = AnnotatedElementUtils.findMergedAnnotation(controllerClass, FolderPrefix.class);
            if (folderPrefix != null && !folderPrefix.value().isEmpty()) {
                folderPath = folderPrefix.value().replaceAll("^/+", "").replaceAll("/+$", "");
            } else {
                folderPath = extractPathFromPackage(controllerClass.getPackageName());
            }
        } else {
            folderPath = extractPathFromPackage(controllerClass.getPackageName());
        }

        // Support Spring 6+ path pattern API
        Set<String> existingPaths;
        if (mapping.getPathPatternsCondition() != null) {
            existingPaths = mapping.getPathPatternsCondition().getPatterns()
                    .stream().map(PathPattern::getPatternString).collect(Collectors.toSet());
        } else if (mapping.getPatternsCondition() != null) {
            existingPaths = mapping.getPatternsCondition().getPatterns();
        } else {
            existingPaths = Set.of(); // fallback to empty
        }

        // Prepend folder path
        Set<String> newPaths = existingPaths.stream().map(path -> {
            String cleanFolderPath = folderPath.replaceAll("^/+", "").replaceAll("/+$", "");
            String cleanPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
            if (cleanFolderPath.isEmpty()) {
                return "/" + cleanPath;
            } else if (cleanPath.isEmpty()) {
                return "/" + cleanFolderPath;
            } else {
                return "/" + cleanFolderPath + "/" + cleanPath;
            }
        }).collect(Collectors.toSet());

        // Create new RequestMappingInfo
        RequestMappingInfo.Builder builder = RequestMappingInfo
                .paths(newPaths.toArray(new String[0]));

        builder.methods(mapping.getMethodsCondition().getMethods().toArray(new RequestMethod[0]));
        builder.params(mapping.getParamsCondition().getExpressions().toArray(new String[0]));
        builder.headers(mapping.getHeadersCondition().getExpressions().toArray(new String[0]));
        builder.consumes(mapping.getConsumesCondition().getExpressions().stream().map(e -> e.getMediaType().toString()).toArray(String[]::new));
        builder.produces(mapping.getProducesCondition().getExpressions().stream().map(e -> e.getMediaType().toString()).toArray(String[]::new));
        if (mapping.getName() != null)
            builder.mappingName(mapping.getName());
        if (mapping.getCustomCondition() != null)
            builder.customCondition(mapping.getCustomCondition());

        RequestMappingInfo finalMapping = builder.build();

        // Add global API prefix if annotated
        if (!AnnotatedElementUtils.hasAnnotation(controllerClass, Sign.class) && !AnnotatedElementUtils.hasAnnotation(controllerClass, Web.class)) {
            RequestMappingInfo apiPrefixMapping = RequestMappingInfo.paths(API_PREFIX).build();
            finalMapping = apiPrefixMapping.combine(finalMapping);
        }

        System.out.println("Mapped " + method.getName() + " to " + newPaths);

        super.registerHandlerMethod(handler, method, finalMapping);
    }

    private String extractPathFromPackage(String fullPackage) {
        if (fullPackage.startsWith(BASE_PACKAGE)) {
            String subPackage = fullPackage.substring(BASE_PACKAGE.length());
            // Remove leading dot if present
            if (subPackage.startsWith(".")) subPackage = subPackage.substring(1);

            // Convert package to path parts
            String[] parts = subPackage.split("\\.");

            // Filter out unwanted parts like "controller"
            String filteredPath = Arrays.stream(parts)
                    .filter(part -> !part.equalsIgnoreCase("controller"))
                    .map(part -> part.replace("_", "-"))
                    .collect(Collectors.joining("/"));

            return filteredPath;
        }
        return "";
    }
}
