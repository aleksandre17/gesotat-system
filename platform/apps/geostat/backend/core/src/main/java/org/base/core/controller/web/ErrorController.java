package org.base.core.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.base.core.anotation.Web;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.context.ApplicationContext;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.function.Predicate;

/**
 * Terminal servlet error endpoint shared by every host application. A host that ships an error
 * template gets that page; a host without one (the API) gets an RFC 9457 body. A view name is never
 * returned for a missing template: it would become a relative forward and re-enter the error path.
 * Exception text and the failing URI are not exposed.
 */
@Controller
@Web
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
    private static final String GENERAL_VIEW = "error/general";

    private final Predicate<String> templateAvailable;

    @org.springframework.beans.factory.annotation.Autowired
    public ErrorController(ApplicationContext context) {
        this(templateAvailability(context));
    }

    private static Predicate<String> templateAvailability(ApplicationContext context) {
        TemplateAvailabilityProviders providers = new TemplateAvailabilityProviders(context);
        return view -> providers.getProvider(view, context) != null;
    }

    ErrorController(Predicate<String> templateAvailable) {
        this.templateAvailable = templateAvailable;
    }

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        return render(resolve(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @RequestMapping("/error/{code}")
    public Object handleErrorCode(@PathVariable String code) {
        return render(resolve(code, HttpStatus.NOT_FOUND));
    }

    private Object render(HttpStatus status) {
        for (String view : new String[]{"error/" + status.value(), GENERAL_VIEW}) {
            if (templateAvailable.test(view)) {
                ModelAndView page = new ModelAndView(view, status);
                page.addObject("status", status.value());
                page.addObject("message", status.getReasonPhrase());
                return page;
            }
        }
        ProblemDetail detail = ProblemDetail.forStatus(status);
        detail.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .cacheControl(CacheControl.noStore())
                .body(detail);
    }

    private static HttpStatus resolve(Object code, HttpStatus fallback) {
        if (code == null) {
            return fallback;
        }
        try {
            HttpStatus status = HttpStatus.resolve(Integer.parseInt(code.toString()));
            return status != null && status.isError() ? status : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
