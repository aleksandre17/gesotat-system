package org.base.core.exeption;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.base.core.exeption.extend.ApiException;
import org.base.core.exeption.extend.ResourceNotFoundException;
import org.base.core.model.response.ApiExceptionResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    public static class ResponseEntityBuilder {
        public static ResponseEntity<?> build(ApiExceptionResponse apiError) {
            return new ResponseEntity<>(apiError, apiError.getStatus());
        }
    }

    private boolean isNotJsonRequest(HttpServletRequest request, HandlerMethod handlerMethod) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) return false;

        if (handlerMethod != null) {
            if (handlerMethod.hasMethodAnnotation(ResponseBody.class)) return false;
            Class<?> beanType = handlerMethod.getBeanType();
            return AnnotationUtils.findAnnotation(beanType, RestController.class) == null;
        }

        return true;
    }

    private HandlerMethod getHandlerMethod(WebRequest request) {
        if (!(request instanceof ServletWebRequest)) {
            return null;
        }

        Object handler = ((ServletWebRequest) request)
                .getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (handler instanceof HandlerMethod) {
            return (HandlerMethod) handler;
        }

        return null;
    }

    public ResponseEntity<?> redirectWeb(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse httpServletResponse,
            ApiExceptionResponse response
    ) {
        try {
            request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, response.getStatus().value());
            request.setAttribute(RequestDispatcher.ERROR_MESSAGE, ex.getMessage());
            request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ex);
            request.getRequestDispatcher("/error").forward(request, httpServletResponse);
            return null;
        } catch (Exception e) {
            return ResponseEntityBuilder.build(response);
        }
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request,
            HttpServletResponse httpServletResponse,
            HandlerMethod handlerMethod
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.",
                Collections.singletonList(ex.getMessage())
        );

        if (isNotJsonRequest(request, handlerMethod)) {
            return redirectWeb(ex, request, httpServletResponse, response);
        }

        return ResponseEntityBuilder.build(response);
    }


    @ExceptionHandler(ApiException.class)
    public final ResponseEntity<?> handleUserNotFoundException(ApiException ex) {
        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ex.getErrorCode().getCode());
        return ResponseEntityBuilder.build(response);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request,
            HttpServletResponse httpServletResponse,
            HandlerMethod handlerMethod
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ex.getErrorCode().getCode());

        if (isNotJsonRequest(request, handlerMethod)) {
            return redirectWeb(ex, request, httpServletResponse, response);
        }

        return ResponseEntityBuilder.build(response);
    }



    @SuppressWarnings("unchecked")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                Collections.singletonList(ex.getMessage())
        );

        HttpServletRequest httpServletRequest = ((ServletWebRequest) request).getNativeRequest(HttpServletRequest.class);
        HttpServletResponse httpServletResponse = ((ServletWebRequest) request).getNativeResponse(HttpServletResponse.class);

        if (isNotJsonRequest(httpServletRequest, getHandlerMethod(request))) {
            return (ResponseEntity<Object>) redirectWeb(ex, httpServletRequest, httpServletResponse, response);
        }

        return (ResponseEntity<Object>) ResponseEntityBuilder.build(response);
    }


    @SuppressWarnings("unchecked")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiExceptionResponse response = new ApiExceptionResponse(
                ex.getStatusCode().value() > 0 ? HttpStatus.valueOf(ex.getStatusCode().value()) : HttpStatus.BAD_REQUEST,
                "Validation failed",
                errors
        );

        HttpServletRequest servletRequest = (HttpServletRequest) ((ServletWebRequest) request).getNativeRequest();
        HttpServletResponse httpServletResponse = ((ServletWebRequest) request).getNativeResponse(HttpServletResponse.class);

        if (isNotJsonRequest(servletRequest, getHandlerMethod(request))) {
            return (ResponseEntity<Object>) redirectWeb(ex, servletRequest, httpServletResponse, response);
        }

        response.setErrorCode(ErrorCode.VALIDATION_ERROR.getCode());
        return (ResponseEntity<Object>) ResponseEntityBuilder.build(response);
    }


    @SuppressWarnings("unchesked")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {

        List<String> details = new ArrayList<String>();
        details.add(ex.getMessage());
        ApiExceptionResponse response = new ApiExceptionResponse(HttpStatus.BAD_REQUEST, "Type Mismatch" , details);

        HttpServletRequest servletRequest = (HttpServletRequest) ((ServletWebRequest) request).getNativeRequest();
        HttpServletResponse httpServletResponse = ((ServletWebRequest) request).getNativeResponse(HttpServletResponse.class);

        if (isNotJsonRequest(servletRequest, getHandlerMethod(request))) {
            return (ResponseEntity<Object>) redirectWeb(ex, servletRequest, httpServletResponse, response);
        }

        return new ResponseEntity<>(response, response.getStatus());
    }


    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        List<String> details = new ArrayList<String>();
        details.add(ex.getParameterName() + " parameter is missing");

        ApiExceptionResponse response = new ApiExceptionResponse(HttpStatus.valueOf(ex.getStatusCode().value()), "Missing Parameters" , details);

        HttpServletRequest servletRequest = (HttpServletRequest) ((ServletWebRequest) request).getNativeRequest();
        HttpServletResponse httpServletResponse = ((ServletWebRequest) request).getNativeResponse(HttpServletResponse.class);


        if (isNotJsonRequest(servletRequest, getHandlerMethod(request))) {
            return (ResponseEntity<Object>) redirectWeb(ex, servletRequest, httpServletResponse, response);
        }

        return new ResponseEntity<>(response, response.getStatus());
    }


    @SuppressWarnings("unchecked")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        List<String> details = new ArrayList<String>();
        details.add(String.format("Could not find the %s method for URL %s", ex.getHttpMethod(), ex.getRequestURL()));
        ApiExceptionResponse response = new ApiExceptionResponse(HttpStatus.valueOf(ex.getStatusCode().value()), "Method Not Found" , details);

        HttpServletRequest servletRequest = (HttpServletRequest) ((ServletWebRequest) request).getNativeRequest();
        HttpServletResponse httpServletResponse = ((ServletWebRequest) request).getNativeResponse(HttpServletResponse.class);

        if (isNotJsonRequest(servletRequest, getHandlerMethod(request))) {
            return (ResponseEntity<Object>) redirectWeb(ex, servletRequest, httpServletResponse, response);
        }
        return new ResponseEntity<>(response, response.getStatus());

    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiExceptionResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request,
            HttpServletResponse httpServletResponse,
            HandlerMethod handlerMethod
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ErrorCode.DEFAULT_ERROR.getCode());

        if (isNotJsonRequest(request, handlerMethod)) {
            return (ResponseEntity<ApiExceptionResponse>) redirectWeb(ex, request, httpServletResponse, response);
        }

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiExceptionResponse> handleAllUncaughtException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse httpServletResponse,
            HandlerMethod handlerMethod
    ) {
        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode());

        if (isNotJsonRequest(request, handlerMethod)) {
            return (ResponseEntity<ApiExceptionResponse>) redirectWeb(ex, request,  httpServletResponse, response);
        }

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiExceptionResponse> handleExpiredJwtException(ExpiredJwtException ex, HttpServletRequest request, HandlerMethod handlerMethod) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.UNAUTHORIZED,
                "JWT token has expired",
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ErrorCode.UNSIGN.getCode());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }


    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(404);
        return new ResponseEntity<>(response, HttpStatusCode.valueOf(404));
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ApiExceptionResponse> handleMalformedJwtException(
            MalformedJwtException ex,
            HttpServletRequest request,
            HandlerMethod handlerMethod
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid JWT token format",
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ErrorCode.UNSIGN.getCode());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<ApiExceptionResponse> handleUnsupportedJwtException(
            UnsupportedJwtException ex,
            HttpServletRequest request,
            HandlerMethod handlerMethod
    ) {
        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "Unsupported JWT token",
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ErrorCode.UNSIGN.getCode());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiExceptionResponse> handleIllegalArgumentException(
            UnsupportedJwtException ex,
            HttpServletRequest request,
            HandlerMethod handlerMethod
    ) {

        ApiExceptionResponse response = new ApiExceptionResponse(
                HttpStatus.BAD_REQUEST,
                "JWT claims string is empty",
                Collections.singletonList(ex.getMessage())
        );
        response.setErrorCode(ErrorCode.UNSIGN.getCode());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


}
