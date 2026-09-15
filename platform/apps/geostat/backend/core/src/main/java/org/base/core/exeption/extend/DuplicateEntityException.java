package org.base.core.exeption.extend;

import org.base.core.exeption.ErrorCode;

public class DuplicateEntityException extends ApiException {
    public DuplicateEntityException(String message) {
        super(message, ErrorCode.DUPLICATE_ENTITY);
    }
}
