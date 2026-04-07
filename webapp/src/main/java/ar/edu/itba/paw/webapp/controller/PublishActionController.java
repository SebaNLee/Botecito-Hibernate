package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.services.ItemService;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublishActionController {

    private final ItemService itemService;

    @Autowired
    public PublishActionController(final ItemService itemService) {
        this.itemService = itemService;
    }

    @RequestMapping(value = "/publish/{token}/delete", method = RequestMethod.GET)
    public ModelAndView deletePublication(@PathVariable("token") final String token) {
        final ModelAndView mav = new ModelAndView("booking-action-result");
        final Optional<Item> item = itemService.findItemByOwnerDeleteToken(token);
        if (item.isEmpty()) {
            mav.addObject("actionTitle", "Publication not found");
            mav.addObject("actionMessage", "The publication token is invalid or no longer available.");
            return mav;
        }

        mav.addObject("itemId", item.get().getId());
        if (item.get().getOwnerDeleteUsedAt() != null
                || !Boolean.TRUE.equals(item.get().getActive())) {
            mav.addObject("actionTitle", "Publication already deleted");
            mav.addObject("actionMessage", "This publication delete link was already used.");
            return mav;
        }

        if (!itemService.deactivateItemByOwnerDeleteToken(token, OffsetDateTime.now())) {
            mav.addObject("actionTitle", "Publication could not be deleted");
            mav.addObject("actionMessage", "Try again or verify that the publication is still active.");
            return mav;
        }

        mav.addObject("actionTitle", "Publication deleted");
        mav.addObject("actionMessage", "Your publication was removed from marketplace results.");
        return mav;
    }
}
