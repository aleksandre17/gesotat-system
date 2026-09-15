package org.base.core.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;

import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class ApiExceptionResponse
{

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private  Date timestamp;
    private HttpStatus status;
    private String message;
    private List<String> errors;
    private int errorCode;


    public ApiExceptionResponse(HttpStatus status, String message, List<String> errors) {
        this.timestamp = new Date();
        this.status = status;
        this.message = message;
        this.errors = errors;
    }

}
