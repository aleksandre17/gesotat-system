package org.base.api.service.platform.statistical.compiler;

/** One machine-readable finding: a stable code, a JSON-pointer-like path and a human explanation. */
public record ContractIssue(Code code, String path, String message) {

    /** Closed vocabulary; clients branch on the code, never on the message. */
    public enum Code {
        MALFORMED_DOCUMENT, UNKNOWN_FIELD, MISSING_FIELD, INVALID_VALUE, INVALID_REFERENCE,
        STRUCTURE_CHOICE_AMBIGUOUS, REDUNDANT_MEASURE_SEMANTICS,
        UNRESOLVED_REFERENCE, REFERENCE_NOT_APPROVED, PROFILE_UNSUPPORTED,
        DUPLICATE_COMPONENT, NO_DIMENSION, NO_MEASURE, UNCODED_DIMENSION_NOT_ALLOWED,
        INVALID_ATTACHMENT, NUMERIC_ENVELOPE_EXCEEDED,
        CONSTANT_BINDING_INVALID, MISSING_CAPTION, CAPABILITY_EXCEEDED, PROVIDER_UNSUPPORTED
    }

    public static ContractIssue of(Code code, String path, String message) { return new ContractIssue(code, path, message); }
}
