package org.base.api.service.artifact;

import java.util.List;

/**
 * {@code SOURCE_PATH}: NFC-normalize the row value, remove {@code stripPrefix}, resolve under
 * {@code packageRoot}. Only exact package paths bind; nothing is guessed.
 */
public record SourcePathMatchRule(String stripPrefix, String packageRoot, List<Binding> bindings) implements ArtifactMatchRule {
    public static final String TYPE = "SOURCE_PATH";

    public SourcePathMatchRule {
        bindings = List.copyOf(bindings);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String resolve(String sourceValue) {
        String value = ArtifactManifestGenerator.normalizePath(sourceValue);
        if (!stripPrefix.isEmpty()) {
            if (!value.startsWith(stripPrefix)) return null;
            value = value.substring(stripPrefix.length());
        }
        return ArtifactManifestGenerator.normalizePath(packageRoot + value);
    }

    @Override
    public boolean covers(String packagePath) {
        return packagePath.startsWith(packageRoot);
    }
}
