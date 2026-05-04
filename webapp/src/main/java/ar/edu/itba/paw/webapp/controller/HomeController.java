package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.util.AvailabilityPickerBuilder;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ItemService itemService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        final ModelAndView mav = new ModelAndView("index");
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerBuilder.build(itemService.listAvailabilities(), itemService.listBookings()));
        return mav;
    }
}
