package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublishController {

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publish(
            @RequestParam(value = "submitted", required = false, defaultValue = "false") final boolean submitted,
            @ModelAttribute("publishForm") final PublishBoatForm form) {
        final ModelAndView mav = new ModelAndView("publish");
        mav.addObject("submitted", submitted);
        mav.addObject("capacityOptions", buildCapacityOptions());
        mav.addObject("weightOptions", buildWeightOptions());
        return mav;
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishSubmit(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return new ModelAndView("redirect:/publish?submitted=true");
    }

    private static Map<String, String> buildCapacityOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("2", "2 personas");
        options.put("4", "4 personas");
        options.put("6", "6 personas");
        options.put("8", "8 personas");
        options.put("10", "10 personas");
        options.put("12", "12 personas");
        return options;
    }

    private static Map<String, String> buildWeightOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("400", "400 kg");
        options.put("600", "600 kg");
        options.put("800", "800 kg");
        options.put("1000", "1,000 kg");
        options.put("1200", "1,200 kg");
        return options;
    }
}
