package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.BlockSlotForm;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.presentation.AvailabilityPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsActionsPresentation;
import ar.edu.itba.paw.webapp.presentation.MyBoatsPresentation;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class MyBoatsController {

    private final MyBoatsPresentation myBoatsPresentation;
    private final MyBoatsActionsPresentation myBoatsActionsPresentation;
    private final PublishActionPresentation publishActionPresentation;
    private final AvailabilityPresentation availabilityPresentation;

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(
            final HttpServletRequest request,
            @RequestParam(value = "page", defaultValue = "1") final int page,
            @RequestParam(value = "pageSize", defaultValue = "12") final int pageSize) {
        return myBoatsPresentation.myBoats(request, page, pageSize);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/edit", method = RequestMethod.GET)
    public ModelAndView editPublicationForm(
            @PathVariable("id") final int itemId,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.editPublicationForm(itemId, request, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/edit", method = RequestMethod.POST)
    public ModelAndView editPublicationSubmit(
            @PathVariable("id") final int itemId,
            @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        return publishActionPresentation.editPublicationSubmit(itemId, form, errors, request, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/disable", method = RequestMethod.POST)
    public ModelAndView disablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.disablePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/enable", method = RequestMethod.POST)
    public ModelAndView enablePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.enablePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/delete", method = RequestMethod.POST)
    public ModelAndView hardDeletePublication(
            @PathVariable("id") final int itemId, final RedirectAttributes redirectAttributes) {
        return myBoatsActionsPresentation.hardDeletePublication(itemId, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final RedirectAttributes redirectAttributes) {
        return availabilityPresentation.manageAvailabilityPage(itemId, requestedDate, returnParam, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability/disable", method = RequestMethod.POST)
    public ModelAndView blockSlot(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnParam,
            @Valid @ModelAttribute final BlockSlotForm blockSlotForm,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return availabilityPresentation.blockSlot(itemId, returnParam, blockSlotForm, errors, redirectAttributes);
    }

    @RequestMapping(value = "/my-boats/{id:[0-9]+}/availability/enable", method = RequestMethod.POST)
    public ModelAndView unblockSlot(
            @PathVariable("id") final int itemId,
            @RequestParam("blockBookingId") final int blockBookingId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final RedirectAttributes redirectAttributes) {
        return availabilityPresentation.unblockSlot(
                itemId, blockBookingId, requestedDate, returnParam, redirectAttributes);
    }
}
