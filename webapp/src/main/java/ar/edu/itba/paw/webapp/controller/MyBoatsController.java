package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.ManageItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ManageAvailabilityQueryForm;
import ar.edu.itba.paw.webapp.form.MyBoatsSearchForm;
import ar.edu.itba.paw.webapp.form.SaveSelfBlocksForm;
import ar.edu.itba.paw.webapp.presentation.AvailabilityPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsActionsPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsPresentation;
import ar.edu.itba.paw.webapp.presentation.ToastPresentation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyBoatsController {

    private static final String MANAGE_AVAILABILITY_MESSAGE_PREFIX = "manageAvailability";

    private final ItemService itemService;
    private final ManageItemService manageItemService;
    private final BookingService bookingService;
    private final MessageSource messageSource;

    @ModelAttribute("myBoatsSearch")
    public MyBoatsSearchForm defaultMyBoatsSearch() {
        final MyBoatsSearchForm form = new MyBoatsSearchForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("myBoatsSearch") final MyBoatsSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return MyBoatsPresentation.myBoatsErrors(search, errors, messageSource);
        }
        final PageModel<Item> itemPage = itemService.listOwnerItems(
                user.getId(),
                search.getSearchQuery(),
                search.getStatus(),
                search.getPage(),
                search.getPageSize(),
                search.getSortBy());
        return MyBoatsPresentation.myBoatsList(search, itemPage);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        manageItemService.setEnabled(itemId, user.getId(), false);
        return MyBoatsActionsPresentation.disablePublicationResult(redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        manageItemService.setEnabled(itemId, user.getId(), true);
        return MyBoatsActionsPresentation.enablePublicationResult(redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        manageItemService.deleteItem(itemId, user.getId());
        return MyBoatsActionsPresentation.hardDeletePublicationResult(redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @ModelAttribute final ManageAvailabilityQueryForm query) {
        final var model = bookingService.getSelfBlocks(itemId, user.getId(), query.getDate());
        return AvailabilityPresentation.manageAvailabilityPage(model);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability/save", method = RequestMethod.POST)
    public ModelAndView saveSelfBlocks(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute final SaveSelfBlocksForm saveSelfBlocksForm,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            final var model = bookingService.getSelfBlocks(itemId, user.getId(), saveSelfBlocksForm.getDate());
            return AvailabilityPresentation.saveSelfBlocksErrors(
                    model,
                    ToastPresentation.validationToasts(errors, MANAGE_AVAILABILITY_MESSAGE_PREFIX, messageSource));
        }
        bookingService.saveSelfBlockChanges(
                itemId,
                user.getId(),
                saveSelfBlocksForm.getDate(),
                saveSelfBlocksForm.getDeletedBlockIds(),
                saveSelfBlocksForm.getUpdates(),
                saveSelfBlocksForm.getCreates());
        return AvailabilityPresentation.saveSelfBlocksSuccess(
                itemId, saveSelfBlocksForm.getDate().toString(), redirectAttributes);
    }
}
