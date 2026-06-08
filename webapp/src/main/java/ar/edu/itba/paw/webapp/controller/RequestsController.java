package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.RejectPaymentForm;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import ar.edu.itba.paw.webapp.presentation.RequestsPresentation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class RequestsController {

    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final MessageSource messageSource;

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
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return RequestsPresentation.outgoingErrors(search, errors, messageSource);
        }
        return RequestsPresentation.outgoing(search, searchOutgoing(user, search), userReviews(user));
    }

    @RequestMapping(value = "/requests/incoming", method = RequestMethod.GET)
    public ModelAndView incoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return RequestsPresentation.incomingErrors(search, errors, messageSource);
        }
        return RequestsPresentation.incoming(search, searchIncoming(user, search), userReviews(user));
    }

    @PostMapping("/requests/outgoing/{bookingId}/review")
    public ModelAndView submitOutgoingReview(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            @Valid @ModelAttribute("reviewForm") final ReviewForm reviewForm,
            final BindingResult reviewErrors,
            final RedirectAttributes redirectAttributes) {
        if (reviewErrors.hasErrors()) {
            return RequestsPresentation.outgoingAfterReview(search, redirectAttributes, true, false);
        }
        final boolean created = reviewService
                .createReviewForBooking(
                        bookingId,
                        user.getId(),
                        reviewForm.getRating(),
                        reviewForm.getComment(),
                        reviewForm.getTargetTypeEnum())
                .isPresent();
        return RequestsPresentation.outgoingAfterReview(search, redirectAttributes, false, created);
    }

    @PostMapping("/requests/incoming/{bookingId}/review")
    public ModelAndView submitIncomingReview(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            @Valid @ModelAttribute("reviewForm") final ReviewForm reviewForm,
            final BindingResult reviewErrors,
            final RedirectAttributes redirectAttributes) {
        if (reviewErrors.hasErrors()) {
            return RequestsPresentation.incomingAfterReview(search, redirectAttributes, true, false);
        }
        final boolean created = reviewService
                .createReviewForBooking(
                        bookingId,
                        user.getId(),
                        reviewForm.getRating(),
                        reviewForm.getComment(),
                        reviewForm.getTargetTypeEnum())
                .isPresent();
        return RequestsPresentation.incomingAfterReview(search, redirectAttributes, false, created);
    }

    @GetMapping("/requests/bookings/{bookingId}/payment-proof")
    public ResponseEntity<byte[]> downloadPaymentProof(
            @AuthenticationPrincipal final BotecitoUserDetails user, @PathVariable("bookingId") final int bookingId) {
        final Optional<PaymentProof> proof = bookingService.getPaymentProofForParticipant(bookingId, user.getId());
        if (proof.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return RequestsPresentation.paymentProofResponse(proof.get());
    }

    @PostMapping("/requests/incoming/{bookingId}/accept")
    public ModelAndView acceptIncoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.acceptBooking(bookingId, user.getId());
        return RequestsPresentation.acceptIncomingBookingResult(search, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject")
    public ModelAndView rejectIncoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.rejectBooking(bookingId, user.getId());
        return RequestsPresentation.rejectIncomingBookingResult(search, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/confirm-payment")
    public ModelAndView confirmIncomingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.confirmPayment(bookingId, user.getId());
        return RequestsPresentation.confirmIncomingPaymentResult(search, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject-payment")
    public ModelAndView rejectIncomingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            @Valid @ModelAttribute final RejectPaymentForm rejectPaymentForm,
            final BindingResult rejectPaymentErrors,
            final RedirectAttributes redirectAttributes) {
        if (rejectPaymentErrors.hasErrors()) {
            return RequestsPresentation.rejectIncomingPaymentMissingReason(search, redirectAttributes);
        }
        bookingService.rejectPayment(bookingId, user.getId(), rejectPaymentForm.getReason());
        return RequestsPresentation.rejectIncomingPaymentResult(search, redirectAttributes);
    }

    @PostMapping("/requests/outgoing/{bookingId}/cancel")
    public ModelAndView cancelOutgoing(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(bookingId, user.getId());
        return RequestsPresentation.cancelOutgoingBookingResult(search, redirectAttributes);
    }

    @PostMapping(value = "/requests/outgoing/{bookingId}/payment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView submitOutgoingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            @Valid @ModelAttribute("paymentProof") final PaymentProofForm paymentProof,
            final BindingResult bindingResult,
            final RedirectAttributes redirectAttributes)
            throws IOException {
        if (bindingResult.hasErrors()) {
            return RequestsPresentation.submitOutgoingPaymentInvalidFile(search, redirectAttributes);
        }
        bookingService.submitPayment(
                bookingId,
                paymentProof.getFileName(),
                paymentProof.getContentType(),
                paymentProof.getFileBytes(),
                paymentProof.getTrimmedGuestReply(),
                user.getId());
        return RequestsPresentation.submitOutgoingPaymentResult(search, redirectAttributes);
    }

    private PageModel<Booking> searchOutgoing(final BotecitoUserDetails user, final BookingSearchForm search) {
        return bookingService.searchBookings(
                user.getId(),
                false,
                search.getSearchQuery(),
                search.getDate(),
                search.getStatus(),
                search.getPage(),
                search.getPageSize(),
                search.getSortBy());
    }

    private PageModel<Booking> searchIncoming(final BotecitoUserDetails user, final BookingSearchForm search) {
        return bookingService.searchBookings(
                user.getId(),
                true,
                search.getSearchQuery(),
                search.getDate(),
                search.getStatus(),
                search.getPage(),
                search.getPageSize(),
                search.getSortBy());
    }

    private Map<Integer, List<Review>> userReviews(final BotecitoUserDetails user) {
        return reviewService.findReviewsByBookingIds(user.getId());
    }
}
