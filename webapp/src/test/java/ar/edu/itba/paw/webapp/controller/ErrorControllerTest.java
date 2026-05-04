package ar.edu.itba.paw.webapp.controller;

import javax.servlet.RequestDispatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class ErrorControllerTest {

    @Test
    public void testRenderErrorStatusAndPath() {
        final ErrorController controller = new ErrorController();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/private");

        final ModelAndView mav = controller.renderError(request, response);

        Assertions.assertEquals(403, response.getStatus());
        Assertions.assertEquals("error", mav.getViewName());
        Assertions.assertEquals(403, mav.getModel().get("httpStatus"));
        Assertions.assertEquals("/private", mav.getModel().get("failedPath"));
    }

    @Test
    public void testRenderErrorFallback() {
        final ErrorController controller = new ErrorController();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, "abc");

        final ModelAndView mav = controller.renderError(request, response);

        Assertions.assertEquals(404, response.getStatus());
        Assertions.assertEquals("error", mav.getViewName());
        Assertions.assertEquals(404, mav.getModel().get("httpStatus"));
    }
}
