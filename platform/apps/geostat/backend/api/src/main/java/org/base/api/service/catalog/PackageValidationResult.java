package org.base.api.service.catalog;

import java.util.List;

public record PackageValidationResult(boolean valid, List<PackageValidationIssue> issues) {
}
