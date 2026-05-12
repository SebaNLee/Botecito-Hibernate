package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.nuevo.BookingSearchForm;
import ar.edu.itba.paw.webapp.presentation.BookingPresentation;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Guest booking requests (nuevo stack). Does not use {@link
 * ar.edu.itba.paw.webapp.controller.support.BookingDashboardMvcSupport}.
 */
@Controller
@RequiredArgsConstructor
public class RequestsController {

    private final BookingPresentation bookingPresentation;

    /**
     * Defaults for GET {@code /requests/outgoing} and {@code /requests/incoming}
     * before binding; omitted query params keep these values.
     */
    @ModelAttribute("bookingSearch")
    public BookingSearchForm defaultBookingSearch() {
        final BookingSearchForm form = new BookingSearchForm();
        form.setPage(1);
        form.setPageSize(12);
        form.setSortBy("newest");
        return form;
    }

    @RequestMapping(value = "/requests", method = RequestMethod.GET)
    public String requestsRoot() {
        return "redirect:/requests/outgoing";
    }

    @RequestMapping(value = "/requests/outgoing", method = RequestMethod.GET)
    public ModelAndView outgoing(
            final HttpServletRequest request,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return bookingPresentation.outgoingBookingsErrors(request, search, errors);
        }
        return bookingPresentation.outgoingBookingsGet(request, search);
    }

    @RequestMapping(value = "/requests/incoming", method = RequestMethod.GET)
    public ModelAndView incoming(
            final HttpServletRequest request,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return bookingPresentation.incomingBookingsErrors(request, search, errors);
        }
        return bookingPresentation.incomingBookingsGet(request, search);
    }

    @GetMapping("/requests/bookings/{bookingId}/payment-proof")
    public ResponseEntity<byte[]> downloadPaymentProof(@PathVariable("bookingId") final int bookingId) {
        return bookingPresentation.downloadPaymentProof(bookingId);
    }

    @PostMapping("/requests/incoming/{bookingId}/accept")
    public ModelAndView acceptIncoming(
            @PathVariable("bookingId") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingPresentation.acceptIncomingBooking(bookingId, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject")
    public ModelAndView rejectIncoming(
            @PathVariable("bookingId") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingPresentation.rejectIncomingBooking(bookingId, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/confirm-payment")
    public ModelAndView confirmIncomingPayment(
            @PathVariable("bookingId") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingPresentation.confirmIncomingPayment(bookingId, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject-payment")
    public ModelAndView rejectIncomingPayment(
            @PathVariable("bookingId") final int bookingId,
            @RequestParam(name = "reason", required = false) final String reason,
            final RedirectAttributes redirectAttributes) {
        return bookingPresentation.rejectIncomingPayment(bookingId, reason, redirectAttributes);
    }

    @PostMapping("/requests/outgoing/{bookingId}/cancel")
    public ModelAndView cancelOutgoing(
            @PathVariable("bookingId") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingPresentation.cancelOutgoingBooking(bookingId, redirectAttributes);
    }

    @PostMapping(value = "/requests/outgoing/{bookingId}/payment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView submitOutgoingPayment(
            @PathVariable("bookingId") final int bookingId,
            @Valid @ModelAttribute("paymentProof") final PaymentProofForm paymentProof,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return bookingPresentation.submitOutgoingPaymentValidationErrors(redirectAttributes);
        }
        return bookingPresentation.submitOutgoingPayment(bookingId, paymentProof, redirectAttributes);
    }
}
