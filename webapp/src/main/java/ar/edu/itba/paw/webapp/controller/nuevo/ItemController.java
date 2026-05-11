package ar.edu.itba.paw.webapp.controller.nuevo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

// TODO: this is a partial migration of ItemController, move the rest as it gets migrated to ORM

@Controller
@RequestMapping("/item")
public class ItemController {

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public ModelAndView createForm() {
        return new ModelAndView("redirect:/publish");
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ModelAndView create() {
        return new ModelAndView("redirect:/publish");
    }
}
