package org.base.api.service.platform.access;

import java.util.List;

/** Immutable in-memory representation of a self-describing Access Semantic Package v3. */
public record SemanticAccessPackage(String productCode, String contractCode, int contractRevision, String packageCode, String packageVersion,
                                    List<SemanticAccessDataset> datasets, List<SemanticAccessField> fields,
                                    List<SemanticAccessKey> keys, List<SemanticAccessRelation> relations,
                                    List<SemanticAccessProjection> projections,
                                    List<SemanticAccessStatisticalBinding> statisticalBindings,
                                    List<SemanticAccessPage> pages,
                                    List<SemanticAccessMetadataSchema> metadataSchemas,
                                    List<SemanticAccessMetadataAssertion> metadataAssertions) {
    public SemanticAccessPackage(String productCode, String contractCode, int contractRevision, String packageCode, String packageVersion,
                                 List<SemanticAccessDataset> datasets, List<SemanticAccessField> fields,
                                 List<SemanticAccessKey> keys, List<SemanticAccessRelation> relations,
                                 List<SemanticAccessProjection> projections,
                                 List<SemanticAccessStatisticalBinding> statisticalBindings) {
        this(productCode, contractCode, contractRevision, packageCode, packageVersion, datasets, fields, keys, relations, projections, statisticalBindings, List.of(), List.of(), List.of());
    }
    public SemanticAccessPackage(String productCode, String contractCode, int contractRevision, String packageCode, String packageVersion,
                                 List<SemanticAccessDataset> datasets, List<SemanticAccessField> fields, List<SemanticAccessKey> keys,
                                 List<SemanticAccessRelation> relations, List<SemanticAccessProjection> projections,
                                 List<SemanticAccessStatisticalBinding> statisticalBindings, List<SemanticAccessPage> pages) {
        this(productCode,contractCode,contractRevision,packageCode,packageVersion,datasets,fields,keys,relations,projections,statisticalBindings,pages,List.of(),List.of());
    }
}
