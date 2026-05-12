package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchModel;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.PaymentProof;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.models.nuevo.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.nuevo.exceptions.NoAnticipationException;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.nuevo.BookingInterface;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.nuevo.BookingSearchForm;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// TODO: Pass try-catch exceptions to ErrorHandler @ControllerAdvice

@Component
@RequiredArgsConstructor
public class BookingPresentation {

    private static final String MESSAGE_PREFIX = "bookingSearch";
    private static final String REDIRECT_INCOMING = "redirect:/requests/incoming";
    private static final String REDIRECT_OUTGOING = "redirect:/requests/outgoing";

    private final BookingInterface bookingInterface;
    private final UserService userService;
    private final ToastPresentation toastPresentation;

    public ModelAndView outgoingBookingsGet(final HttpServletRequest request, final BookingSearchForm search) {
        return currentUserId()
                .map(userId -> {
                    final OutcomingSearch outcoming = toOutcomingSearch(search, userId);
                    final BookingSearchResult result = bookingInterface.searchOutcomingBookings(outcoming);
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("nuevo/requests-outgoing", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView outgoingBookingsErrors(
            final HttpServletRequest request, final BookingSearchForm search, final BindingResult errors) {
        if (currentUserId().isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("nuevo/requests-outgoing", "bookingSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView incomingBookingsGet(final HttpServletRequest request, final BookingSearchForm search) {
        return currentUserId()
                .map(userId -> {
                    final IncomingSearch incoming = toIncomingSearch(search, userId);
                    final BookingSearchResult result = bookingInterface.searchIncomingBookings(incoming);
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("nuevo/requests-incoming", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView incomingBookingsErrors(
            final HttpServletRequest request, final BookingSearchForm search, final BindingResult errors) {
        if (currentUserId().isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("nuevo/requests-incoming", "bookingSearch", search);
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView acceptIncomingBooking(final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        try {
            bookingInterface.acceptBooking(bookingId, callerId.get());
            ToastSupport.success(redirectAttributes, "requests.booking.accepted");
        } catch (final NoAnticipationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.noAnticipation");
        } catch (final IllegalBookingOperationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.operationFailed");
        }
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingBooking(final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        try {
            bookingInterface.rejectBooking(bookingId, callerId.get());
            ToastSupport.warning(redirectAttributes, "requests.booking.rejected");
        } catch (final NoAnticipationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.noAnticipation");
        } catch (final IllegalBookingOperationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.operationFailed");
        }
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView confirmIncomingPayment(final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        try {
            bookingInterface.confirmPayment(bookingId, callerId.get());
            ToastSupport.success(redirectAttributes, "requests.booking.paymentConfirmed");
        } catch (final NoAnticipationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.noAnticipation");
        } catch (final IllegalBookingOperationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.operationFailed");
        }
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView rejectIncomingPayment(
            final int bookingId, final String reason, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final String trimmed = reason != null ? reason.trim() : "";
        if (!StringUtils.hasText(trimmed)) {
            ToastSupport.error(redirectAttributes, "requests.booking.rejectPaymentReasonRequired");
            return new ModelAndView(REDIRECT_INCOMING);
        }
        final String bounded = trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
        try {
            bookingInterface.rejectPayment(bookingId, callerId.get(), bounded);
            ToastSupport.success(redirectAttributes, "requests.booking.paymentRefused");
        } catch (final NoAnticipationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.noAnticipation");
        } catch (final IllegalBookingOperationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.operationFailed");
        }
        return new ModelAndView(REDIRECT_INCOMING);
    }

    public ModelAndView cancelOutgoingBooking(final int bookingId, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        try {
            bookingInterface.cancelBooking(bookingId, callerId.get());
            ToastSupport.success(redirectAttributes, "requests.booking.cancelled");
        } catch (final IllegalBookingOperationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.operationFailed");
        }
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ModelAndView submitOutgoingPayment(
            final int bookingId, final PaymentProofForm form, final RedirectAttributes redirectAttributes) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
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
        final PaymentProof proof = PaymentProof.builder()
                .bookingId(bookingId)
                .fileName(fileName)
                .contentType(contentType)
                .fileData(fileData)
                .replyMsg(guestReply)
                .build();
        try {
            bookingInterface.submitPayment(proof, callerId.get());
            ToastSupport.success(redirectAttributes, "requests.booking.paymentSubmitted");
        } catch (final NoAnticipationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.noAnticipation");
        } catch (final IllegalBookingOperationException e) {
            ToastSupport.error(redirectAttributes, "requests.booking.operationFailed");
        }
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ModelAndView submitOutgoingPaymentValidationErrors(final RedirectAttributes redirectAttributes) {
        ToastSupport.error(redirectAttributes, "requests.booking.paymentInvalidFile");
        return new ModelAndView(REDIRECT_OUTGOING);
    }

    public ResponseEntity<byte[]> downloadPaymentProof(final int bookingId) {
        final Optional<Integer> callerId = currentUserId();
        if (callerId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final Optional<PaymentProof> proof = bookingInterface.getPaymentProofForParticipant(bookingId, callerId.get());
        if (proof.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        final PaymentProof p = proof.get();
        if (p.getFileData() == null || p.getFileData().length == 0) {
            return ResponseEntity.notFound().build();
        }
        final String contentType =
                StringUtils.hasText(p.getContentType()) ? p.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        final String rawName = StringUtils.hasText(p.getFileName()) ? p.getFileName() : "proof";
        final String fileName = rawName.replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentLength(p.getFileData().length)
                .body(p.getFileData());
    }

    private void addListingModelObjects(
            final ModelAndView mav, final BookingSearchForm search, final List<Booking> bookings, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("bookingPage", new Page<>(bookings, page, pageSize, totalItems));
        mav.addObject("bookingsCount", totalItems);
        mav.addObject("sort", sortRequestValue(search.getSortBy()));
    }

    private static String sortRequestValue(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "newest";
        }
        return sortBy;
    }

    public IncomingSearch toIncomingSearch(final BookingSearchForm form, final int hostId) {
        final IncomingSearch incoming = new IncomingSearch();
        incoming.setHostId(hostId);
        final BookingSearchModel criteria = new BookingSearchModel();
        copyFormToCriteria(form, criteria);
        incoming.setSearch(criteria);
        return incoming;
    }

    public OutcomingSearch toOutcomingSearch(final BookingSearchForm form, final int guestId) {
        final OutcomingSearch outcoming = new OutcomingSearch();
        outcoming.setGuestId(guestId);
        final BookingSearchModel criteria = new BookingSearchModel();
        copyFormToCriteria(form, criteria);
        outcoming.setSearch(criteria);
        return outcoming;
    }

    private static void copyFormToCriteria(final BookingSearchForm form, final BookingSearchModel criteria) {
        criteria.setSearchQuery(form.getSearchQuery());
        criteria.setDate(PresentationUtils.parseDate(form.getDate()));
        criteria.setStatus(parseStatus(form.getStatus()));
        criteria.setPage(form.getPage());
        criteria.setPageSize(form.getPageSize());
        criteria.setSortBy(form.getSortBy());
    }

    private static BookingStatus parseStatus(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return BookingStatus.valueOf(raw.trim());
    }

    private Optional<Integer> currentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName()).flatMap(u -> Optional.ofNullable(u.getId()));
    }
}
