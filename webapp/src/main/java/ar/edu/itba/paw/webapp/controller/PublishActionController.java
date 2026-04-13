package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import java.time.OffsetDateTime;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublishActionController {

    private final ItemService itemService;
    private final UserService userService;

    public PublishActionController(final ItemService itemService, final UserService userService) {
        this.itemService = itemService;
        this.userService = userService;
    }

    @RequestMapping(value = "/publish/{token}/confirm", method = RequestMethod.GET)
    public ModelAndView confirmPublication(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/publish/{token}/delete", method = RequestMethod.GET)
    public ModelAndView deletePublication(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/publish/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView deletePublicationInAccount(@PathVariable("id") final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final var item = itemService.findItemById(itemId).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/profile?publishAction=forbidden");
        }
        if (!Boolean.TRUE.equals(item.getActive())) {
            return new ModelAndView("redirect:/profile?publishAction=alreadyDeleted");
        }
        if (item.getOwnerDeleteToken() == null || item.getOwnerDeleteToken().isBlank()) {
            return new ModelAndView("redirect:/profile?publishAction=error");
        }

        final boolean deleted =
                itemService.deactivateItemByOwnerDeleteToken(item.getOwnerDeleteToken(), OffsetDateTime.now());
        if (!deleted) {
            return new ModelAndView("redirect:/profile?publishAction=error");
        }
        return new ModelAndView("redirect:/profile?publishAction=deleted");
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
