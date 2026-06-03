package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.SearchResult;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class BookingPresentation {

    private static final String MESSAGE_PREFIX = "bookingSearch";
    private static final String REDIRECT_INCOMING = "redirect:/requests/incoming";
    private static final String REDIRECT_OUTGOING = "redirect:/requests/outgoing";

    private final ToastPresentation toastPresentation;

    public ModelAndView outgoingBookings(
            final BookingSearchForm search,
            final SearchResult<Booking> result,
            final Map<Integer, List<Review>> userReviews) {
        final List<Booking> bookings = result.getPageElements();
        final ModelAndView mav = new ModelAndView("requests-outgoing", "bookingSearch", search);
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, result.getTotalCount());
        addUserReviews(mav, userReviews);
        return mav;
    }

    public ModelAndView outgoingBookingsErrors(final BookingSearchForm search, final BindingResult errors) {
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("requests-outgoing", "bookingSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView incomingBookings(
            final BookingSearchForm search,
            final SearchResult<Booking> result,
            final Map<Integer, List<Review>> userReviews) {
        final List<Booking> bookings = result.getPageElements();
        final ModelAndView mav = new ModelAndView("requests-incoming", "bookingSearch", search);
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, result.getTotalCount());
        addUserReviews(mav, userReviews);
        return mav;
    }

    public ModelAndView incomingBookingsErrors(final BookingSearchForm search, final BindingResult errors) {
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("requests-incoming", "bookingSearch", search);
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView acceptIncomingBookingResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.accepted");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingBookingResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.warning(redirectAttributes, "requests.booking.rejected");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView confirmIncomingPaymentResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.paymentConfirmed");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingPaymentMissingReason(final RedirectAttributes redirectAttributes) {
        ToastSupport.error(redirectAttributes, "requests.booking.rejectPaymentReasonRequired");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingPaymentResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.paymentRefused");
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView cancelOutgoingBookingResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.cancelled");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ModelAndView submitOutgoingPaymentInvalidFile(final RedirectAttributes redirectAttributes) {
        ToastSupport.error(redirectAttributes, "requests.booking.paymentInvalidFile");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ModelAndView submitOutgoingPaymentResult(final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.paymentSubmitted");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ResponseEntity<byte[]> paymentProofResponse(final PaymentProof proof) {
        if (proof.getFileData() == null || proof.getFileData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        final String contentType = StringUtils.hasText(proof.getContentType())
                ? proof.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        final String rawName = StringUtils.hasText(proof.getFilename()) ? proof.getFilename() : "proof";
        final String fileName = rawName.replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentLength(proof.getFileData().length)
                .body(proof.getFileData());
    }

    private void addUserReviews(final ModelAndView mav, final Map<Integer, List<Review>> userReviews) {
        mav.addObject(
                "userReviews", userReviews == null || userReviews.isEmpty() ? Collections.emptyMap() : userReviews);
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
