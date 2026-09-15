package org.base.core.entity.data;

public enum ImportJobStatus {
    RECEIVED,
    CATALOGED,
    VALIDATING,
    VALIDATED,
    IMPORTING,
    PARTIAL_SUCCESS,
    SUCCESS,
    FAILED,
    CANCELLED
}
