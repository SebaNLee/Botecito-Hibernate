package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.BookingDashboardMvcSupport;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingDashboardMvcSupport bookingDashboardMvcSupport;

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public ModelAndView dashboard() {
        return new ModelAndView("redirect:/my-boats");
    }

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(
            @RequestParam(value = "status", required = false) final List<String> status,
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int page,
            final HttpServletRequest request) {
        return bookingDashboardMvcSupport.myBoats(status, query, page, request);
    }

    @RequestMapping(value = "/bookings", method = RequestMethod.GET)
    public ModelAndView bookings(
            @RequestParam(value = "status", required = false) final List<String> status,
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int page,
            final HttpServletRequest request) {
        return bookingDashboardMvcSupport.guestBookings(status, query, page, request);
    }

    @RequestMapping(value = "/profile/dashboard", method = RequestMethod.GET)
    public ModelAndView legacyDashboardRedirect() {
        return new ModelAndView("redirect:/my-boats");
    }
}
