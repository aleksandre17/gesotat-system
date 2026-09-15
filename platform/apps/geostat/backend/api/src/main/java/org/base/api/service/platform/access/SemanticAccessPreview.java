package org.base.api.service.platform.access;

import java.util.List;

public record SemanticAccessPreview(String productCode, String packageCode, String packageVersion,
                                    boolean valid, List<SemanticAccessIssue> issues,
                                    int datasets, int fields, int relations, int projections) {
}
