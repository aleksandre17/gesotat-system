package org.base.api.service.artifact;

import java.util.List;

/**
 * Strategy that derives package paths from source row values for one declared relation. New rule
 * types are added as a new implementation plus an {@link ArtifactMatchRuleParser}; the matcher and
 * the rest of the line stay unchanged (Open/Closed).
 */
public interface ArtifactMatchRule {

    /** One row field feeding one language slot of the relation. */
    record Binding(String language, String field, boolean required) {}

    /** Declared type code, e.g. {@code SOURCE_PATH}. */
    String type();

    List<Binding> bindings();

    /** Package path for a source value, {@code null} when the value is outside this rule's scope. */
    String resolve(String sourceValue);

    /** Whether a package entry belongs to this rule's scope (unreferenced covered entries are orphans). */
    boolean covers(String packagePath);
}
