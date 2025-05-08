package org.base.core.controller.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.base.core.anotation.Web;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.NoHandlerFoundException;

@Controller
@Web
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Exception exception = (Exception) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        model.addAttribute("status", status);
        model.addAttribute("message", errorMessage != null ? errorMessage : "An error occurred");
        model.addAttribute("exception", exception);
        model.addAttribute("url", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));

        if (exception != null) {
            if (exception instanceof AccessDeniedException) {
                model.addAttribute("message", "Access Denied");
                return "error/403";
            } else if (exception instanceof NoHandlerFoundException) {
                model.addAttribute("message", "Page Not Found");
                return "error/404";
            }
        }

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            return switch (statusCode) {
                case 404 -> "error/404";
                case 403 -> "error/403";
                case 500 -> "error/500";
                default -> "error/general";
            };
        }

        return "error/general";
    }


    @RequestMapping("/error/{code}")
    public String handleErrorCode(@PathVariable String code) {
        return "error/" + code;
    }


}

