package org.base.api.service.artifact;

/**
 * Approved control-plane declaration of how rows of one dataset version attach to artifacts.
 * Cardinality is counted per row and language slot.
 */
public record ArtifactRelationDefinition(long datasetVersionId, String relationCode, ArtifactRole artifactRole, ArtifactPolicy policy,
                                         int minPerRow, Integer maxPerRow, boolean ordered, ArtifactMatchRule matchRule) {
    public ArtifactRelationDefinition {
        if (relationCode == null || !relationCode.matches("[A-Z0-9_]{1,64}")) throw new IllegalArgumentException("Invalid relation code");
        if (minPerRow < 0 || (maxPerRow != null && (maxPerRow < 1 || maxPerRow < minPerRow))) throw new IllegalArgumentException("Invalid cardinality");
    }
}
