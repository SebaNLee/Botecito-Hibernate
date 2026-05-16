package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.services.nuevo.MarketplaceInterface;
import ar.edu.itba.paw.webapp.util.nuevo.AvailabilityJsonHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final MarketplaceInterface marketplaceInterface;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        final var data = marketplaceInterface.buildHomeAvailabilityData();
        final ModelAndView mav = new ModelAndView("index");
        AvailabilityJsonHelper.addAvailabilityPickerData(mav, "search", data);
        return mav;
    }
}
