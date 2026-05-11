package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.BookingDashboardMvcSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingDashboardMvcSupport bookingDashboardMvcSupport;

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public ModelAndView dashboard() {
        return new ModelAndView("redirect:/my-boats");
    }

    @RequestMapping(value = "/profile/dashboard", method = RequestMethod.GET)
    public ModelAndView legacyDashboardRedirect() {
        return new ModelAndView("redirect:/my-boats");
    }
}
