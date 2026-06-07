package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.webapp.form.MyBoatsSearchForm;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MyBoatsPresentation {

    private static final String MESSAGE_PREFIX = "myBoats";

    private final ToastPresentation toastPresentation;

    public ModelAndView myBoatsList(
            final MyBoatsSearchForm search, final List<Item> ownedItems, final long totalCount) {
        final ModelAndView mav = new ModelAndView("my-boats", "myBoatsSearch", search);
        addListingModelObjects(mav, search, ownedItems, totalCount);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView myBoatsErrors(final MyBoatsSearchForm search, final BindingResult errors) {
        final List<Item> ownedItems = List.of();
        final ModelAndView mav = new ModelAndView("my-boats", "myBoatsSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("ownedItems", ownedItems);
        mav.addObject("itemPage", new PageModel<>(ownedItems, 1, 12, 0));
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        mav.addObject("hasValidationErrors", true);
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MyBoatsSearchForm search, final List<Item> ownedItems, final long total) {
        final int page = search.getPage();
        final int pageSize = search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;

        mav.addObject("ownedItems", ownedItems);
        mav.addObject("itemPage", new PageModel<>(ownedItems, page, pageSize, totalItems));
    }
}
