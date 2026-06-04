package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.presentation.BookingPresentation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RequestsController {

    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final BookingPresentation bookingPresentation;

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
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return bookingPresentation.outgoingBookingsErrors(search, errors);
        }
        final SearchResult<Booking> result = bookingService.searchBookings(
                user.getId(),
                false,
                search.getSearchQuery(),
                search.getDate(),
                search.getStatus(),
                search.getPage(),
                search.getPageSize(),
                search.getSortBy());
        final Map<Integer, List<Review>> userReviews = reviewService.findReviewsByBookingIds(user.getId());
        return bookingPresentation.outgoingBookings(search, result, userReviews);
    }

    @RequestMapping(value = "/requests/incoming", method = RequestMethod.GET)
    public ModelAndView incoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            final HttpServletRequest request,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return bookingPresentation.incomingBookingsErrors(search, errors);
        }
        final SearchResult<Booking> result = bookingService.searchBookings(
                user.getId(),
                true,
                search.getSearchQuery(),
                search.getDate(),
                search.getStatus(),
                search.getPage(),
                search.getPageSize(),
                search.getSortBy());
        final Map<Integer, List<Review>> userReviews = reviewService.findReviewsByBookingIds(user.getId());
        return bookingPresentation.incomingBookings(search, result, userReviews);
    }

    @GetMapping("/requests/bookings/{bookingId}/payment-proof")
    public ResponseEntity<byte[]> downloadPaymentProof(
            @AuthenticationPrincipal final BotecitoUserDetails user, @PathVariable("bookingId") final int bookingId) {
        final Optional<PaymentProof> proof = bookingService.getPaymentProofForParticipant(bookingId, user.getId());
        if (proof.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return bookingPresentation.paymentProofResponse(proof.get());
    }

    @PostMapping("/requests/incoming/{bookingId}/accept")
    public ModelAndView acceptIncoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            final RedirectAttributes redirectAttributes) {
        bookingService.acceptBooking(bookingId, user.getId());
        return bookingPresentation.acceptIncomingBookingResult(redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject")
    public ModelAndView rejectIncoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            final RedirectAttributes redirectAttributes) {
        bookingService.rejectBooking(bookingId, user.getId());
        return bookingPresentation.rejectIncomingBookingResult(redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/confirm-payment")
    public ModelAndView confirmIncomingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            final RedirectAttributes redirectAttributes) {
        bookingService.confirmPayment(bookingId, user.getId());
        return bookingPresentation.confirmIncomingPaymentResult(redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject-payment")
    public ModelAndView rejectIncomingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @RequestParam(name = "reason", required = false) final String reason,
            final RedirectAttributes redirectAttributes) {
        final String trimmed = reason != null ? reason.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            return bookingPresentation.rejectIncomingPaymentMissingReason(redirectAttributes);
        }
        final String bounded = trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
        bookingService.rejectPayment(bookingId, user.getId(), bounded);
        return bookingPresentation.rejectIncomingPaymentResult(redirectAttributes);
    }

    @PostMapping("/requests/outgoing/{bookingId}/cancel")
    public ModelAndView cancelOutgoing(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            final RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(bookingId, user.getId());
        return bookingPresentation.cancelOutgoingBookingResult(redirectAttributes);
    }

    @PostMapping(value = "/requests/outgoing/{bookingId}/payment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView submitOutgoingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @Valid @ModelAttribute("paymentProof") final PaymentProofForm paymentProof,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes)
            throws IOException {
        if (bindingResult.hasErrors()) {
            return bookingPresentation.submitOutgoingPaymentInvalidFile(redirectAttributes);
        }
        final MultipartFile file = paymentProof.getFile();
        final String fileName =
                file != null && StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "proof";
        final String contentType = file != null && StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        final String guestReply = StringUtils.hasText(paymentProof.getGuestReply())
                ? paymentProof.getGuestReply().trim()
                : null;

        bookingService.submitPayment(
                bookingId, fileName, contentType, file != null ? file.getBytes() : null, guestReply, user.getId());
        return bookingPresentation.submitOutgoingPaymentResult(redirectAttributes);
    }
}
