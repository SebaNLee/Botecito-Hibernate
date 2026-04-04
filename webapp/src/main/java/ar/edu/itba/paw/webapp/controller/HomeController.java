package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController {

    private final ItemCatalogService itemCatalogService;

    @Autowired
    public HomeController(final ItemCatalogService itemCatalogService) {
        this.itemCatalogService = itemCatalogService;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        final ModelAndView mav = new ModelAndView("index");
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemCatalogService.listAvailabilities(), itemCatalogService.listBookings()));
        return mav;
    }
}
