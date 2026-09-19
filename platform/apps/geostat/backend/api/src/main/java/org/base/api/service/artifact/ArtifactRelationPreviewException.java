package org.base.api.service.artifact;

import java.util.List;

/** A contract-bound package whose rows cannot be deterministically matched to its entries; nothing was written. */
public class ArtifactRelationPreviewException extends IllegalArgumentException {
    private final transient List<ArtifactRelationPreview.RelationResult> relations;

    public ArtifactRelationPreviewException(List<ArtifactRelationPreview.RelationResult> relations, int errorCount) {
        super("Package relation preview is blocked by " + errorCount + " error(s)");
        this.relations = List.copyOf(relations);
    }

    public List<ArtifactRelationPreview.RelationResult> relations() {
        return relations;
    }
}
