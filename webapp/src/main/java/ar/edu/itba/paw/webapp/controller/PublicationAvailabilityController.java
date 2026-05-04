package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.PublicationAvailabilityMvcSupport;
import ar.edu.itba.paw.webapp.form.BlockSlotForm;
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
public class PublicationAvailabilityController {

    private final PublicationAvailabilityMvcSupport publicationAvailabilityMvcSupport;

    @ModelAttribute("blockSlotForm")
    public BlockSlotForm blockSlotForm() {
        return new BlockSlotForm();
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability", method = RequestMethod.GET)
    public ModelAndView manageAvailability(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final RedirectAttributes redirectAttributes) {
        return publicationAvailabilityMvcSupport.manageAvailabilityPage(
                itemId, requestedDate, returnParam, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability/disable", method = RequestMethod.POST)
    public ModelAndView blockSlot(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "return", required = false) final String returnParam,
            @Valid @ModelAttribute("blockSlotForm") final BlockSlotForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return publicationAvailabilityMvcSupport.blockSlot(itemId, returnParam, form, errors, redirectAttributes);
    }

    @RequestMapping(value = "/profile/item/{id:[0-9]+}/availability/enable", method = RequestMethod.POST)
    public ModelAndView unblockSlot(
            @PathVariable("id") final int itemId,
            @RequestParam("blockBookingId") final int blockBookingId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "return", required = false) final String returnParam,
            final RedirectAttributes redirectAttributes) {
        return publicationAvailabilityMvcSupport.unblockSlot(
                itemId, blockBookingId, requestedDate, returnParam, redirectAttributes);
    }
}
