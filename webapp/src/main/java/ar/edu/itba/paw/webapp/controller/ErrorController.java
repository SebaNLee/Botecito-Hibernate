package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.ErrorPageSupport;
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
        final int status = ErrorPageSupport.resolveErrorStatus(request);
        response.setStatus(status);
        return ErrorPageSupport.modelAndView(status, ErrorPageSupport.resolveFailedPath(request));
    }
}
