package ar.edu.itba.paw.webapp.controller;

import org.springframework.web.servlet.ModelAndView;

/** Builds the shared model for HTTP error views (4xx / 5xx). */
public final class ErrorPageModel {

    private ErrorPageModel() {}

    public static ModelAndView build(final int status, final String failedPath) {
        final ModelAndView mav = new ModelAndView("error");
        mav.addObject("httpStatus", status);
        mav.addObject("errorTitleCode", titleMessageCode(status));
        mav.addObject("errorMessageCode", bodyMessageCode(status));
        mav.addObject("clientSideError", status >= 400 && status < 500);
        if (failedPath != null && !failedPath.isBlank()) {
            mav.addObject("failedPath", failedPath);
        }
        return mav;
    }

    private static String titleMessageCode(final int status) {
        return switch (status) {
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
