package org.base.api.service.catalog;

/** One actionable validation result, safe to return in an import report. */
public record PackageValidationIssue(String code, String subject, String message) {
}
