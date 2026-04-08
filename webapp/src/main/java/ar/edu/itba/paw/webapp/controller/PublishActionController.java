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

    @RequestMapping(value = "/publish/{token}/confirm", method = RequestMethod.GET)
    public ModelAndView confirmPublication(@PathVariable("token") final String token) {
        final ModelAndView mav = new ModelAndView("booking-action-result");
        final Optional<Item> item = itemService.findItemByOwnerDeleteToken(token);
        if (item.isEmpty()) {
            mav.addObject("actionTitleCode", "publishAction.confirmNotFound.title");
            mav.addObject("actionMessageCode", "publishAction.confirmNotFound.message");
            return mav;
        }

        mav.addObject("itemId", item.get().getId());
        if (Boolean.TRUE.equals(item.get().getActive())) {
            mav.addObject("actionTitleCode", "publishAction.alreadyConfirmed.title");
            mav.addObject("actionMessageCode", "publishAction.alreadyConfirmed.message");
            return mav;
        }

        if (!itemService.activateItemByOwnerDeleteToken(token)) {
            mav.addObject("actionTitleCode", "publishAction.confirmFailed.title");
            mav.addObject("actionMessageCode", "publishAction.confirmFailed.message");
            return mav;
        }
        mav.addObject("actionTitleCode", "publishAction.confirmed.title");
        mav.addObject("actionMessageCode", "publishAction.confirmed.message");
        return mav;
    }

    @RequestMapping(value = "/publish/{token}/delete", method = RequestMethod.GET)
    public ModelAndView deletePublication(@PathVariable("token") final String token) {
        final ModelAndView mav = new ModelAndView("booking-action-result");
        final Optional<Item> item = itemService.findItemByOwnerDeleteToken(token);
        if (item.isEmpty()) {
            mav.addObject("actionTitleCode", "publishAction.notFound.title");
            mav.addObject("actionMessageCode", "publishAction.notFound.message");
            return mav;
        }

        mav.addObject("itemId", item.get().getId());
        if (item.get().getOwnerDeleteUsedAt() != null
                || !Boolean.TRUE.equals(item.get().getActive())) {
            mav.addObject("actionTitleCode", "publishAction.alreadyDeleted.title");
            mav.addObject("actionMessageCode", "publishAction.alreadyDeleted.message");
            return mav;
        }

        if (!itemService.deactivateItemByOwnerDeleteToken(token, OffsetDateTime.now())) {
            mav.addObject("actionTitleCode", "publishAction.deleteFailed.title");
            mav.addObject("actionMessageCode", "publishAction.deleteFailed.message");
            return mav;
        }

        mav.addObject("actionTitleCode", "publishAction.deleted.title");
        mav.addObject("actionMessageCode", "publishAction.deleted.message");
        return mav;
    }
}
