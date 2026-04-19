package ar.edu.itba.paw.webapp.controller;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorController {

    @RequestMapping(value = "/errors", method = RequestMethod.GET)
    public ModelAndView renderError(final HttpServletRequest request, final HttpServletResponse response) {
        final int status = resolveStatus(request);
        response.setStatus(status);

        final Object uriAttr = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        final String failedPath = uriAttr != null ? uriAttr.toString() : null;

        return ErrorPageModel.build(status, failedPath);
    }

    private static int resolveStatus(final HttpServletRequest request) {
        final Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusAttr instanceof Integer) {
            return (Integer) statusAttr;
        }
        if (statusAttr instanceof String) {
            try {
                return Integer.parseInt((String) statusAttr);
            } catch (final NumberFormatException ignored) {
                return 404;
            }
        }
        return 404;
    }
}
