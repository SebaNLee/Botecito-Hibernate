package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.presentation.MyBoatsActionsPresentation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyBoatsActionController {

    private final MyBoatsActionsPresentation myBoatsActionsPresentation;

    // TODO edit after bookings, redirects now. Not implemented
    @RequestMapping(
            value = "/profile/item/{id:[0-9]+}/edit",
            method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView editNotAvailable(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return new ModelAndView("redirect:/my-boats");
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.disablePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.enablePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.hardDeletePublication(itemId, redirectAttributes);
    }
}
