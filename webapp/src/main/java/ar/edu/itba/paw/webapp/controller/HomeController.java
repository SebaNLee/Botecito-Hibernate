package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.DisabledTimeSlotService;
import ar.edu.itba.paw.services.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

    private final ItemService itemService;
    private final DisabledTimeSlotService disabledTimeSlotService;

    @Autowired
    public HomeController(final ItemService itemService, final DisabledTimeSlotService disabledTimeSlotService) {
        this.itemService = itemService;
        this.disabledTimeSlotService = disabledTimeSlotService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        final ModelAndView mav = new ModelAndView("index");
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilities(),
                        itemService.listBookings(),
                        disabledTimeSlotService.listAll()));
        return mav;
    }
}
