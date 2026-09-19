package org.base.core.controller.web;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.base.core.exeption.GlobalExceptionHandler;
import org.base.core.model.response.ApiExceptionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The servlet error path must terminate: no view name without a template, no forward from a non-REQUEST dispatch. */
class ErrorPathTerminationTest {

    private final ErrorController controller = new ErrorController(view -> false);
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void errorDispatchRendersProblemBodyWithoutLeakingExceptionOrUri() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "secret internal detail");
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/unmapped");

        ResponseEntity<ProblemDetail> response = problem(controller.handleError(request));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals("Not Found", response.getBody().getTitle());
        assertNull(response.getBody().getDetail());
        assertNull(response.getBody().getInstance());
    }

    @Test
    void missingOrNonErrorStatusFallsBackTo500() {
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, problem(controller.handleError(new MockHttpServletRequest())).getStatusCode());

        MockHttpServletRequest ok = new MockHttpServletRequest();
        ok.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 200);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, problem(controller.handleError(ok)).getStatusCode());
    }

    @Test
    void errorCodeRouteAcceptsOnlyErrorStatuses() {
        assertEquals(HttpStatus.FORBIDDEN, problem(controller.handleErrorCode("403")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, problem(controller.handleErrorCode("200")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, problem(controller.handleErrorCode("error")).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, problem(controller.handleErrorCode("999")).getStatusCode());
    }

    @Test
    void hostWithTemplatesKeepsItsPagesAndFallsBackToTheGeneralPage() {
        ErrorController web = new ErrorController(java.util.Set.of("error/404", "error/general")::contains);

        ModelAndView specific = (ModelAndView) web.handleErrorCode("404");
        assertEquals("error/404", specific.getViewName());
        assertEquals(HttpStatus.NOT_FOUND, specific.getStatus());

        ModelAndView general = (ModelAndView) web.handleErrorCode("409");
        assertEquals("error/general", general.getViewName());
        assertEquals(409, general.getModel().get("status"));
        assertNull(general.getModel().get("exception"));
    }

    @Test
    void failureInsideErrorOrForwardDispatchIsNotForwardedAgain() {
        for (DispatcherType type : List.of(DispatcherType.ERROR, DispatcherType.FORWARD)) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error/error/404");
            request.setDispatcherType(type);
            MockHttpServletResponse servletResponse = new MockHttpServletResponse();

            ResponseEntity<?> response = handler.redirectWeb(new IllegalStateException("x"), request, servletResponse, notFound());

            assertNotNull(response);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(servletResponse.getForwardedUrl());
        }
    }

    @Test
    void initialRequestIsStillForwardedToTheErrorEndpoint() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unmapped");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ResponseEntity<?> response = handler.redirectWeb(new IllegalStateException("x"), request, servletResponse, notFound());

        assertNull(response);
        assertEquals("/error", servletResponse.getForwardedUrl());
        assertEquals(404, request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
    }

    @SuppressWarnings("unchecked")
    private static ResponseEntity<ProblemDetail> problem(Object rendered) {
        return (ResponseEntity<ProblemDetail>) rendered;
    }

    private static ApiExceptionResponse notFound() {
        return new ApiExceptionResponse(HttpStatus.NOT_FOUND, "Resource not found", List.of("Resource not found"));
    }
}
