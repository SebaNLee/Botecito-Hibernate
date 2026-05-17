package ar.edu.itba.paw.webapp.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorsController {

    // TODO impl feedback: do not give user info about missing/existent resources

    @RequestMapping("/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView error500() {
        return errorModelAndView(500);
    }

    @RequestMapping("/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView error403() {
        return errorModelAndView(403);
    }

    @RequestMapping("/404")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView error404() {
        return errorModelAndView(404);
    }

    @RequestMapping("/400")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView error400(final HttpServletRequest request) {
        ModelAndView mav = errorModelAndView(400);
        Object maxUpload = request.getAttribute("maxUploadSizeExceeded");
        mav.addObject("maxUploadSizeExceeded", Boolean.TRUE.equals(maxUpload));
        return mav;
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
            case 400 -> "error.400.title";
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
            case 400 -> "error.400.message";
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
