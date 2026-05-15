package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.nuevo.ErrorsController;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

public class ErrorControllerTest {

    private final ErrorsController controller = new ErrorsController();

    @Test
    public void testError404() {
        final ModelAndView mav = controller.error404();
        Assertions.assertEquals("error", mav.getViewName());
        Assertions.assertEquals(404, mav.getModel().get("httpStatus"));
        Assertions.assertEquals("error.404.title", mav.getModel().get("errorTitleCode"));
    }

    @Test
    public void testError403() {
        final ModelAndView mav = controller.error403();
        Assertions.assertEquals("error", mav.getViewName());
        Assertions.assertEquals(403, mav.getModel().get("httpStatus"));
        Assertions.assertEquals("error.403.title", mav.getModel().get("errorTitleCode"));
    }

    @Test
    public void testError500() {
        final ModelAndView mav = controller.error500();
        Assertions.assertEquals("error", mav.getViewName());
        Assertions.assertEquals(500, mav.getModel().get("httpStatus"));
        Assertions.assertEquals("error.500.title", mav.getModel().get("errorTitleCode"));
    }

    @Test
    public void testError400() {
        final HttpServletRequest request = new MockHttpServletRequest();
        final ModelAndView mav = controller.error400(request);
        Assertions.assertEquals("error", mav.getViewName());
        Assertions.assertEquals(400, mav.getModel().get("httpStatus"));
        Assertions.assertEquals(false, mav.getModel().get("maxUploadSizeExceeded"));
    }

    @Test
    public void testError400WithMaxUpload() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("maxUploadSizeExceeded", true);
        final ModelAndView mav = controller.error400(request);
        Assertions.assertEquals(true, mav.getModel().get("maxUploadSizeExceeded"));
    }
}
