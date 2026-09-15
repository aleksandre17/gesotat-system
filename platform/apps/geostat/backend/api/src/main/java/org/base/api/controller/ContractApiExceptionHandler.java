package org.base.api.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RFC 9457 problem-details boundary for governed contract endpoints. */
@RestControllerAdvice(assignableTypes={ContractQueryController.class,CanonicalPageDataController.class,DynamicDataController.class,ContractIntrospectionController.class,ContractDiscoveryController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContractApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ContractApiExceptionHandler.class);
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> invalid(IllegalArgumentException ex, jakarta.servlet.http.HttpServletRequest request){return problem(HttpStatus.BAD_REQUEST,"contract-query-invalid",ex.getMessage(),request);}
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> data(DataAccessException ex, jakarta.servlet.http.HttpServletRequest request){log.error("Contract data query failed",ex);return problem(HttpStatus.SERVICE_UNAVAILABLE,"contract-data-unavailable","Governed data is temporarily unavailable",request);}
    private ResponseEntity<ProblemDetail> problem(HttpStatus status,String code,String detail,jakarta.servlet.http.HttpServletRequest request){ProblemDetail p=ProblemDetail.forStatusAndDetail(status,detail==null?status.getReasonPhrase():detail);p.setType(URI.create("https://api.geostat.ge/problems/"+code));p.setTitle(code);p.setProperty("code",code);p.setProperty("instance",request.getRequestURI());p.setProperty("correlationId",request.getHeader("X-Correlation-Id")!=null?request.getHeader("X-Correlation-Id"):UUID.randomUUID().toString());return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).header(HttpHeaders.CACHE_CONTROL,"no-store").body(p);}
}
