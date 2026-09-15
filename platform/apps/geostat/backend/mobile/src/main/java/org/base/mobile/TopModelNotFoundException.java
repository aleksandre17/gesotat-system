package org.base.mobile;

import org.base.core.exeption.ErrorCode;
import org.base.core.exeption.extend.ApiException;

public class TopModelNotFoundException extends ApiException {
    public TopModelNotFoundException() {
        super("Top model not found", ErrorCode.valueOf("No model found with the given filters"));
    }
}
