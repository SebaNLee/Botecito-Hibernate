package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.webapp.form.BookingSearchForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class RequestsPresentation {

    private static final String BOOKING_SEARCH_PREFIX = "bookingSearch";
    private static final String REDIRECT_INCOMING = "redirect:/requests/incoming";
    private static final String REDIRECT_OUTGOING = "redirect:/requests/outgoing";

    private RequestsPresentation() {}

    public static ModelAndView outgoing(
            final BookingSearchForm search,
            final PageModel<Booking> bookingPage,
            final Map<Integer, List<Review>> userReviews) {
        final ModelAndView mav = new ModelAndView("requests-outgoing", "bookingSearch", search);
        mav.addObject("bookingPage", bookingPage);
        addUserReviews(mav, userReviews);
        return mav;
    }

    public static ModelAndView outgoingErrors(
            final BookingSearchForm search, final BindingResult errors, final MessageSource messageSource) {
        final ModelAndView mav = new ModelAndView("requests-outgoing", "bookingSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("bookingPage", new PageModel<>(List.of(), 1, 12, 0L));
        mav.addObject("toasts", ToastPresentation.validationToasts(errors, BOOKING_SEARCH_PREFIX, messageSource));
        return mav;
    }

    public static ModelAndView incoming(
            final BookingSearchForm search,
            final PageModel<Booking> bookingPage,
            final Map<Integer, List<Review>> userReviews) {
        final ModelAndView mav = new ModelAndView("requests-incoming", "bookingSearch", search);
        mav.addObject("bookingPage", bookingPage);
        addUserReviews(mav, userReviews);
        return mav;
    }

    public static ModelAndView incomingErrors(
            final BookingSearchForm search, final BindingResult errors, final MessageSource messageSource) {
        final ModelAndView mav = new ModelAndView("requests-incoming", "bookingSearch", search);
        mav.addObject("bookingPage", new PageModel<>(List.of(), 1, 12, 0L));
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", ToastPresentation.validationToasts(errors, BOOKING_SEARCH_PREFIX, messageSource));
        return mav;
    }

    public static ModelAndView outgoingAfterReview(
            final BookingSearchForm search,
            final RedirectAttributes redirectAttributes,
            final boolean validationFailed,
            final boolean created) {
        addBookingSearchParams(redirectAttributes, search);
        addReviewFlash(redirectAttributes, validationFailed, created);
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public static ModelAndView incomingAfterReview(
            final BookingSearchForm search,
            final RedirectAttributes redirectAttributes,
            final boolean validationFailed,
            final boolean created) {
        addBookingSearchParams(redirectAttributes, search);
        addReviewFlash(redirectAttributes, validationFailed, created);
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public static ModelAndView acceptIncomingBookingResult(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.accepted");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public static ModelAndView rejectIncomingBookingResult(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.warning(redirectAttributes, "requests.booking.rejected");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public static ModelAndView confirmIncomingPaymentResult(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.paymentConfirmed");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public static ModelAndView rejectIncomingPaymentMissingReason(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.error(redirectAttributes, "requests.booking.rejectPaymentHostMessageRequired");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public static ModelAndView rejectIncomingPaymentResult(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.paymentRefused");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public static ModelAndView cancelOutgoingBookingResult(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.cancelled");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public static ModelAndView submitOutgoingPaymentInvalidFile(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.error(redirectAttributes, "requests.booking.paymentInvalidFile");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public static ModelAndView submitOutgoingPaymentResult(
            final BookingSearchForm search, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "requests.booking.paymentSubmitted");
        addBookingSearchParams(redirectAttributes, search);
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public static ResponseEntity<byte[]> paymentProofResponse(final PaymentProof proof) {
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

    private static void addBookingSearchParams(
            final RedirectAttributes redirectAttributes, final BookingSearchForm search) {
        redirectAttributes.addAttribute("page", search.getPage());
        redirectAttributes.addAttribute("pageSize", search.getPageSize());
        if (!"newest".equals(search.getSortBy())) {
            redirectAttributes.addAttribute("sortBy", search.getSortBy());
        }
        if (StringUtils.hasText(search.getSearchQuery())) {
            redirectAttributes.addAttribute("searchQuery", search.getSearchQuery());
        }
        if (StringUtils.hasText(search.getDateParam())) {
            redirectAttributes.addAttribute("date", search.getDateParam());
        }
        if (StringUtils.hasText(search.getStatusParam())) {
            redirectAttributes.addAttribute("status", search.getStatusParam());
        }
    }

    private static void addReviewFlash(
            final RedirectAttributes redirectAttributes, final boolean validationFailed, final boolean created) {
        if (validationFailed) {
            ToastSupport.error(redirectAttributes, "settings.reviews.validationError");
        } else if (created) {
            ToastSupport.success(redirectAttributes, "settings.reviews.created");
        } else {
            ToastSupport.error(redirectAttributes, "settings.reviews.error");
        }
    }

    private static void addUserReviews(final ModelAndView mav, final Map<Integer, List<Review>> userReviews) {
        mav.addObject(
                "userReviews", userReviews == null || userReviews.isEmpty() ? Collections.emptyMap() : userReviews);
    }
}
