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

    public ModelAndView myBoatsList(final MyBoatsSearchForm search, final PageModel<Item> itemPage) {
        final ModelAndView mav = new ModelAndView("my-boats", "myBoatsSearch", search);
        mav.addObject("itemPage", itemPage);
        mav.addObject("hasValidationErrors", false);
        return mav;
    }

    public ModelAndView myBoatsErrors(final MyBoatsSearchForm search, final BindingResult errors) {
        final ModelAndView mav = new ModelAndView("my-boats", "myBoatsSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("itemPage", new PageModel<>(List.of(), 1, 12, 0L));
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        mav.addObject("hasValidationErrors", true);
        return mav;
    }
}
