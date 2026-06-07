package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MarketplacePresentation {

    private static final String MESSAGE_PREFIX = "marketplace";

    private final ToastPresentation toastPresentation;

    public ModelAndView marketplace(final MarketplaceSearchForm form, final PageModel<Item> itemPage) {
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        mav.addObject("itemPage", itemPage);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView marketplaceErrors(final MarketplaceSearchForm form, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", form);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        mav.addObject("itemPage", new PageModel<Item>(List.of(), 1, 12, 0L));
        mav.addObject("hasValidationErrors", true);
        return mav;
    }
}
