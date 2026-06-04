package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.ManageItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.MyBoatsSearchForm;
import ar.edu.itba.paw.webapp.form.SaveSelfBlocksForm;
import ar.edu.itba.paw.webapp.presentation.AvailabilityPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsActionsPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyBoatsController {

    private final ItemService itemService;
    private final ManageItemService manageItemService;
    private final BookingService bookingService;
    private final MyBoatsPresentation myBoatsPresentation;
    private final MyBoatsActionsPresentation myBoatsActionsPresentation;
    private final AvailabilityPresentation availabilityPresentation;

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
            final HttpServletRequest request,
            @Valid @ModelAttribute("myBoatsSearch") final MyBoatsSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return myBoatsPresentation.myBoatsErrors(request, search, errors);
        }
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final SearchResult<Item> result = itemService.listOwnerItems(
                user.getId(), search.getSearchQuery(), search.getStatus(), page, pageSize, search.getSortBy());
        return myBoatsPresentation.myBoatsList(request, search, result.getPageElements(), result.getTotalCount());
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        manageItemService.setEnabled(itemId, user.getId(), false);
        return myBoatsActionsPresentation.disablePublicationResult(redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        manageItemService.setEnabled(itemId, user.getId(), true);
        return myBoatsActionsPresentation.enablePublicationResult(redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            final RedirectAttributes redirectAttributes) {
        manageItemService.deleteItem(itemId, user.getId());
        return myBoatsActionsPresentation.hardDeletePublicationResult(redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final HttpServletRequest request) {
        final String backPath = AvailabilityPresentation.resolveBackPath(request, returnParam);
        final var model = bookingService.getSelfBlocks(
                itemId, user.getId(), AvailabilityPresentation.parseRequestedDate(requestedDate));
        return availabilityPresentation.manageAvailabilityPage(model, backPath);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability/save", method = RequestMethod.POST)
    public ModelAndView saveSelfBlocks(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnParam,
            @Valid @ModelAttribute final SaveSelfBlocksForm saveSelfBlocksForm,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final String backPath = AvailabilityPresentation.sanitizeReturnPath(returnParam);
        if (errors.hasErrors()) {
            final var model = bookingService.getSelfBlocks(itemId, user.getId(), saveSelfBlocksForm.getDate());
            return availabilityPresentation.saveSelfBlocksErrors(model, backPath, errors);
        }
        bookingService.saveSelfBlockChanges(
                itemId,
                user.getId(),
                saveSelfBlocksForm.getDate(),
                saveSelfBlocksForm.deletedBlockIds(),
                saveSelfBlocksForm.updates(),
                saveSelfBlocksForm.creates());
        return availabilityPresentation.saveSelfBlocksSuccess(
                itemId, saveSelfBlocksForm.getDate().toString(), backPath, redirectAttributes);
    }
}
