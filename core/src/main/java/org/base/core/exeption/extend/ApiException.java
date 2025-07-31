package org.base.core.exeption.extend;

import lombok.Getter;
import org.base.core.exeption.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class ApiException extends RuntimeException {

    private  ErrorCode errorCode;

    public ApiException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(String message, String queryExecutionFailed, Exception e, ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public ApiException(String s, String queryExecutionFailed, Exception e) {
    }

    public ApiException(String couldntRetrieveData, String noYearDataAvailable) {
    }

    public String getClientMessage() {
        return null;
    }
}
