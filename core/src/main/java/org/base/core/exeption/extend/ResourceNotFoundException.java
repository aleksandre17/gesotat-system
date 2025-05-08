package org.base.core.exeption.extend;

import org.base.core.exeption.ErrorCode;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
    }
}
