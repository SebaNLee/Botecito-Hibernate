package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.nuevo.EditPublicationForm;
import ar.edu.itba.paw.webapp.presentation.PublishActionPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class PublishActionController {

    private final PublishActionPresentation publishActionPresentation;

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/edit", method = RequestMethod.GET)
    public ModelAndView editPublicationForm(
            @PathVariable("id") final int itemId,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.editPublicationForm(itemId, request, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/edit", method = RequestMethod.POST)
    public ModelAndView editPublicationSubmit(
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("editForm") final EditPublicationForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.editPublicationSubmit(itemId, form, errors, request, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.disablePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.enablePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.hardDeletePublication(itemId, redirectAttributes);
    }
}
