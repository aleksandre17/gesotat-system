package org.base.core.exeption;

public enum ErrorCode {
    VALIDATION_ERROR(1001),
    SIG_ERROR(1002),
    RESOURCE_NOT_FOUND(1003),
    DUPLICATE_ENTITY(1004),
    INTERNAL_SERVER_ERROR(5000),
    DEFAULT_ERROR(1111),
    UNSIGN(401);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

