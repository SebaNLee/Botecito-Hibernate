package ar.edu.itba.paw.webapp.controller;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorsController {

    @RequestMapping("/errors")
    public ModelAndView errors(
            final HttpServletRequest request,
            final HttpServletResponse response,
            @RequestParam(value = "status", required = false) final String statusParam) {
        final int status = resolveStatus(request, statusParam);
        response.setStatus(status);
        return errorModelAndView(status);
    }

    private static int resolveStatus(final HttpServletRequest request, final String statusParam) {
        final Object errorStatus = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (errorStatus instanceof Integer code) {
            return code;
        }
        if (errorStatus instanceof Number number) {
            return number.intValue();
        }
        if (statusParam != null && !statusParam.isBlank()) {
            try {
                return Integer.parseInt(statusParam.trim());
            } catch (final NumberFormatException ignored) {
                // fall through to default
            }
        }
        return 404;
    }

    private static ModelAndView errorModelAndView(final int status) {
        return new ModelAndView("error")
                .addObject("httpStatus", status)
                .addObject("errorTitleCode", titleMessageCode(status))
                .addObject("errorMessageCode", bodyMessageCode(status))
                .addObject("clientSideError", status >= 400 && status < 500);
    }

    private static String titleMessageCode(final int status) {
        return switch (status) {
            case 403 -> "error.403.title";
            case 404 -> "error.404.title";
            case 405 -> "error.405.title";
            case 408 -> "error.408.title";
            case 409 -> "error.409.title";
            case 413 -> "error.413.title";
            case 415 -> "error.415.title";
            case 429 -> "error.429.title";
            case 500 -> "error.500.title";
            case 502 -> "error.502.title";
            case 503 -> "error.503.title";
            case 504 -> "error.504.title";
            default -> status >= 400 && status < 500 ? "error.4xx.title" : "error.5xx.title";
        };
    }

    private static String bodyMessageCode(final int status) {
        return switch (status) {
            case 403 -> "error.403.message";
            case 404 -> "error.404.message";
            case 405 -> "error.405.message";
            case 408 -> "error.408.message";
            case 409 -> "error.409.message";
            case 413 -> "error.413.message";
            case 415 -> "error.415.message";
            case 429 -> "error.429.message";
            case 500 -> "error.500.message";
            case 502 -> "error.502.message";
            case 503 -> "error.503.message";
            case 504 -> "error.504.message";
            default -> status >= 400 && status < 500 ? "error.4xx.message" : "error.5xx.message";
        };
    }
}
