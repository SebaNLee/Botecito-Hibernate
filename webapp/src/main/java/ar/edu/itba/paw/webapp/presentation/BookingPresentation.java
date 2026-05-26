package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.BookingSearchResult;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class BookingPresentation {

    private static final String MESSAGE_PREFIX = "bookingSearch";
    private static final String REDIRECT_INCOMING = "redirect:/requests/incoming";
    private static final String REDIRECT_OUTGOING = "redirect:/requests/outgoing";

    private final BookingService bookingInterface;
    private final ReviewService reviewInterface;
    private final ToastPresentation toastPresentation;

    public ModelAndView outgoingBookingsGet(
            final BotecitoUserDetails principal, final HttpServletRequest request, final BookingSearchForm search) {
        return callerId(principal)
                .map(userId -> {
                    final BookingSearchResult result = bookingInterface.searchBookings(
                            userId,
                            false,
                            search.getSearchQuery(),
                            search.getDate(),
                            search.getStatus(),
                            search.getPage(),
                            search.getPageSize(),
                            search.getSortBy());
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("requests-outgoing", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    addUserReviews(mav, userId);
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView outgoingBookingsErrors(
            final BotecitoUserDetails principal,
            final HttpServletRequest request,
            final BookingSearchForm search,
            final BindingResult errors) {
        if (callerId(principal).isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("requests-outgoing", "bookingSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView incomingBookingsGet(
            final BotecitoUserDetails principal, final HttpServletRequest request, final BookingSearchForm search) {
        return callerId(principal)
                .map(userId -> {
                    final BookingSearchResult result = bookingInterface.searchBookings(
                            userId,
                            true,
                            search.getSearchQuery(),
                            search.getDate(),
                            search.getStatus(),
                            search.getPage(),
                            search.getPageSize(),
                            search.getSortBy());
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("requests-incoming", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    addUserReviews(mav, userId);
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView incomingBookingsErrors(
            final BotecitoUserDetails principal,
            final HttpServletRequest request,
            final BookingSearchForm search,
            final BindingResult errors) {
        if (callerId(principal).isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("requests-incoming", "bookingSearch", search);
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView acceptIncomingBooking(
            final BotecitoUserDetails principal, final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        bookingInterface.acceptBooking(bookingId, userId.get());
        ToastSupport.success(redirectAttributes, "requests.booking.accepted");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingBooking(
            final BotecitoUserDetails principal, final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        bookingInterface.rejectBooking(bookingId, userId.get());
        ToastSupport.warning(redirectAttributes, "requests.booking.rejected");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView confirmIncomingPayment(
            final BotecitoUserDetails principal, final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        bookingInterface.confirmPayment(bookingId, userId.get());
        ToastSupport.success(redirectAttributes, "requests.booking.paymentConfirmed");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingPayment(
            final BotecitoUserDetails principal,
            final int bookingId,
            final String reason,
            final RedirectAttributes redirectAttributes) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final String trimmed = reason != null ? reason.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            ToastSupport.error(redirectAttributes, "requests.booking.rejectPaymentReasonRequired");
            return new ModelAndView(REDIRECT_INCOMING);
        }
        final String bounded = trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
        bookingInterface.rejectPayment(bookingId, userId.get(), bounded);
        ToastSupport.success(redirectAttributes, "requests.booking.paymentRefused");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView cancelOutgoingBooking(
            final BotecitoUserDetails principal, final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        bookingInterface.cancelBooking(bookingId, userId.get());
        ToastSupport.success(redirectAttributes, "requests.booking.cancelled");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ModelAndView submitOutgoingPayment(
            final BotecitoUserDetails principal,
            final int bookingId,
            final PaymentProofForm form,
            final RedirectAttributes redirectAttributes) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final MultipartFile file = form != null ? form.getFile() : null;
        if (file == null || file.isEmpty()) {
            ToastSupport.error(redirectAttributes, "requests.booking.paymentProofRequired");
            return new ModelAndView(REDIRECT_OUTGOING);
        }
        final byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (final IOException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.paymentInvalidFile");
            return new ModelAndView(REDIRECT_OUTGOING);
        }
        final String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "proof";
        final String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        final String guestReply = form != null && StringUtils.hasText(form.getGuestReply())
                ? form.getGuestReply().trim()
                : null;

        bookingInterface.submitPayment(bookingId, fileName, contentType, fileData, guestReply, userId.get());

        ToastSupport.success(redirectAttributes, "requests.booking.paymentSubmitted");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ModelAndView submitOutgoingPaymentValidationErrors(final RedirectAttributes redirectAttributes) {
        ToastSupport.error(redirectAttributes, "requests.booking.paymentInvalidFile");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ResponseEntity<byte[]> downloadPaymentProof(final BotecitoUserDetails principal, final int bookingId) {
        final Optional<Integer> userId = callerId(principal);
        if (userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final Optional<PaymentProof> proof = bookingInterface.getPaymentProofForParticipant(bookingId, userId.get());
        if (proof.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final PaymentProof p = proof.get();
        if (p.getFileData() == null || p.getFileData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        final String contentType =
                StringUtils.hasText(p.getContentType()) ? p.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        final String rawName = StringUtils.hasText(p.getFilename()) ? p.getFilename() : "proof";
        final String fileName = rawName.replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentLength(p.getFileData().length)
                .body(p.getFileData());
    }

    private void addUserReviews(final ModelAndView mav, final int userId) {
        final Map<Integer, List<Review>> userReviews = reviewInterface.findReviewsByBookingIds(userId);
        mav.addObject("userReviews", userReviews.isEmpty() ? Collections.emptyMap() : userReviews);
    }

    private static Optional<Integer> callerId(final BotecitoUserDetails principal) {
        return principal == null ? Optional.empty() : Optional.of(principal.getId());
    }

    private void addListingModelObjects(
            final ModelAndView mav, final BookingSearchForm search, final List<Booking> bookings, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("bookingPage", new PageModel<>(bookings, page, pageSize, totalItems));
        mav.addObject("bookingsCount", totalItems);
        mav.addObject("sort", sortRequestValue(search.getSortBy()));
    }

    private static String sortRequestValue(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "newest";
        }
        return sortBy;
    }
}
