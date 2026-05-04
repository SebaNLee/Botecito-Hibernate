package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.webapp.controller.support.BookingRequestActionMvcSupport;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.RefusePaymentForm;
import java.io.IOException;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
public class BookingRequestActionController {

    private final BookingRequestActionMvcSupport bookingRequestActionMvcSupport;

    @RequestMapping(value = "/bookings/{token}/accept", method = RequestMethod.GET)
    public ModelAndView acceptBookingRequest(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/bookings/{token}/decline", method = RequestMethod.GET)
    public ModelAndView declineBookingRequest(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/accept", method = RequestMethod.POST)
    public ModelAndView acceptBookingRequestInAccount(
            @PathVariable("id") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingRequestActionMvcSupport.resolveBookingRequestInAccount(
                bookingId, BookingState.BOOKING_CONFIRMED, redirectAttributes);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/decline", method = RequestMethod.POST)
    public ModelAndView declineBookingRequestInAccount(
            @PathVariable("id") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingRequestActionMvcSupport.resolveBookingRequestInAccount(
                bookingId, BookingState.BOOKING_REJECTED, redirectAttributes);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment-proof", method = RequestMethod.POST)
    public ModelAndView submitPaymentProof(
            @PathVariable("id") final int bookingId,
            @ModelAttribute("paymentProofForm") final PaymentProofForm form,
            final RedirectAttributes redirectAttributes)
            throws IOException {
        return bookingRequestActionMvcSupport.submitPaymentProof(bookingId, form, redirectAttributes);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment/refuse", method = RequestMethod.POST)
    public ModelAndView refusePaymentProof(
            @PathVariable("id") final int bookingId,
            @Valid @ModelAttribute("refusePaymentForm") final RefusePaymentForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return bookingRequestActionMvcSupport.refusePaymentProof(bookingId, form, errors, redirectAttributes);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment-proof", method = RequestMethod.GET)
    public ResponseEntity<byte[]> downloadPaymentProof(@PathVariable("id") final int bookingId) {
        return bookingRequestActionMvcSupport.downloadPaymentProof(bookingId);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment/confirm", method = RequestMethod.POST)
    public ModelAndView confirmPaymentReceived(
            @PathVariable("id") final int bookingId, final RedirectAttributes redirectAttributes) {
        return bookingRequestActionMvcSupport.confirmPaymentReceived(bookingId, redirectAttributes);
    }
}
