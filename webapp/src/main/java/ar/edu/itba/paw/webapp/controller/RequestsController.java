package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import ar.edu.itba.paw.webapp.presentation.RequestsPresentation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final RequestsPresentation requestsPresentation;

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
            return requestsPresentation.outgoingErrors(search, errors);
        }
        return requestsPresentation.outgoing(search, searchOutgoing(user, search), userReviews(user));
    }

    @RequestMapping(value = "/requests/incoming", method = RequestMethod.GET)
    public ModelAndView incoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @Valid @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return requestsPresentation.incomingErrors(search, errors);
        }
        return requestsPresentation.incoming(search, searchIncoming(user, search), userReviews(user));
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
            return requestsPresentation.outgoingAfterReview(search, redirectAttributes, true, false);
        }
        final boolean created = reviewService
                .createReviewForBooking(
                        bookingId,
                        user.getId(),
                        reviewForm.getRating(),
                        reviewForm.getComment(),
                        parseTargetType(reviewForm.getTargetType()))
                .isPresent();
        return requestsPresentation.outgoingAfterReview(search, redirectAttributes, false, created);
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
            return requestsPresentation.incomingAfterReview(search, redirectAttributes, true, false);
        }
        final boolean created = reviewService
                .createReviewForBooking(
                        bookingId,
                        user.getId(),
                        reviewForm.getRating(),
                        reviewForm.getComment(),
                        parseTargetType(reviewForm.getTargetType()))
                .isPresent();
        return requestsPresentation.incomingAfterReview(search, redirectAttributes, false, created);
    }

    @GetMapping("/requests/bookings/{bookingId}/payment-proof")
    public ResponseEntity<byte[]> downloadPaymentProof(
            @AuthenticationPrincipal final BotecitoUserDetails user, @PathVariable("bookingId") final int bookingId) {
        final Optional<PaymentProof> proof = bookingService.getPaymentProofForParticipant(bookingId, user.getId());
        if (proof.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return requestsPresentation.paymentProofResponse(proof.get());
    }

    @PostMapping("/requests/incoming/{bookingId}/accept")
    public ModelAndView acceptIncoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.acceptBooking(bookingId, user.getId());
        return requestsPresentation.acceptIncomingBookingResult(search, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject")
    public ModelAndView rejectIncoming(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.rejectBooking(bookingId, user.getId());
        return requestsPresentation.rejectIncomingBookingResult(search, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/confirm-payment")
    public ModelAndView confirmIncomingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.confirmPayment(bookingId, user.getId());
        return requestsPresentation.confirmIncomingPaymentResult(search, redirectAttributes);
    }

    @PostMapping("/requests/incoming/{bookingId}/reject-payment")
    public ModelAndView rejectIncomingPayment(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            @RequestParam(name = "reason", required = false) final String reason,
            final RedirectAttributes redirectAttributes) {
        final String trimmed = reason != null ? reason.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            return requestsPresentation.rejectIncomingPaymentMissingReason(search, redirectAttributes);
        }
        final String bounded = trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
        bookingService.rejectPayment(bookingId, user.getId(), bounded);
        return requestsPresentation.rejectIncomingPaymentResult(search, redirectAttributes);
    }

    @PostMapping("/requests/outgoing/{bookingId}/cancel")
    public ModelAndView cancelOutgoing(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("bookingId") final int bookingId,
            @ModelAttribute("bookingSearch") final BookingSearchForm search,
            final RedirectAttributes redirectAttributes) {
        bookingService.cancelBooking(bookingId, user.getId());
        return requestsPresentation.cancelOutgoingBookingResult(search, redirectAttributes);
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
            return requestsPresentation.submitOutgoingPaymentInvalidFile(search, redirectAttributes);
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
        return requestsPresentation.submitOutgoingPaymentResult(search, redirectAttributes);
    }

    private SearchResult<Booking> searchOutgoing(final BotecitoUserDetails user, final BookingSearchForm search) {
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

    private SearchResult<Booking> searchIncoming(final BotecitoUserDetails user, final BookingSearchForm search) {
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

    private static TargetEnum parseTargetType(final String targetType) {
        if (targetType == null || targetType.isBlank()) {
            return null;
        }
        return TargetEnum.valueOf(targetType.trim());
    }
}
