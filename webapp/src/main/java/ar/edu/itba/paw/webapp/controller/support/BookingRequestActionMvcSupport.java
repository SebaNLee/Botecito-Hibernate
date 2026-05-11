package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.RefusePaymentForm;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public final class BookingRequestActionMvcSupport {

    private static final String DASHBOARD_BOOKINGS_REDIRECT = "redirect:/requests/outgoing";
    private static final String DASHBOARD_HOSTING_REDIRECT = "redirect:/my-boats#received-booking-requests";

    private final BookingRequestService bookingRequestService;
    private final UserService userService;

    public ModelAndView submitPaymentProof(
            final int bookingId, final PaymentProofForm form, final RedirectAttributes redirectAttributes)
            throws IOException {
        final Optional<User> user = currentUser();
        if (user.isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        return submitPaymentProofForUser(bookingId, user.get(), form, redirectAttributes);
    }

    public ModelAndView refusePaymentProof(
            final int bookingId,
            final RefusePaymentForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        return currentUser()
                .map(user -> refusePaymentProofForUser(bookingId, user, form, errors, redirectAttributes))
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ResponseEntity<byte[]> downloadPaymentProof(final int bookingId) {
        return currentUser()
                .map(user -> downloadPaymentProofForUser(bookingId, user))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    public ModelAndView confirmPaymentReceived(final int bookingId, final RedirectAttributes redirectAttributes) {
        return currentUser()
                .map(user -> confirmPaymentReceivedForUser(bookingId, user, redirectAttributes))
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView resolveBookingRequestInAccount(
            final int bookingId, final BookingState bookingState, final RedirectAttributes redirectAttributes) {
        return currentUser()
                .map(user -> resolveBookingRequestForUser(bookingId, user, bookingState, redirectAttributes))
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    private ModelAndView submitPaymentProofForUser(
            final int bookingId,
            final User user,
            final PaymentProofForm form,
            final RedirectAttributes redirectAttributes)
            throws IOException {
        final MultipartFile file = form.getFile();
        final InputStream fileStream =
                file == null || file.isEmpty() ? InputStream.nullInputStream() : file.getInputStream();

        final BookingRequestService.PaymentProofSubmissionOutcome outcome =
                bookingRequestService.submitPaymentProofInAccount(
                        bookingId,
                        user.getId(),
                        fileStream,
                        file == null ? null : file.getOriginalFilename(),
                        file == null ? null : file.getContentType(),
                        form.getGuestReply());
        if (outcome == BookingRequestService.PaymentProofSubmissionOutcome.INVALID_FILE) {
            ToastSupport.error(redirectAttributes, "profile.payment.invalidFile");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }
        if (outcome == BookingRequestService.PaymentProofSubmissionOutcome.ERROR) {
            ToastSupport.error(redirectAttributes, "profile.payment.error");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }
        if (outcome == BookingRequestService.PaymentProofSubmissionOutcome.RESUBMITTED) {
            ToastSupport.success(redirectAttributes, "profile.payment.resubmitted");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }
        if (outcome != BookingRequestService.PaymentProofSubmissionOutcome.SUBMITTED) {
            ToastSupport.error(redirectAttributes, "profile.payment.invalidFile");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }
        ToastSupport.success(redirectAttributes, "profile.payment.submitted");
        return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
    }

    private ModelAndView refusePaymentProofForUser(
            final int bookingId,
            final User user,
            final RefusePaymentForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            ToastSupport.error(redirectAttributes, "profile.payment.refuseError");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        final BookingRequestService.PaymentRefusalOutcome outcome =
                bookingRequestService.refusePaymentProofInAccount(bookingId, user.getId(), form.getReason());
        if (outcome != BookingRequestService.PaymentRefusalOutcome.REFUSED) {
            ToastSupport.error(redirectAttributes, "profile.payment.refuseError");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        ToastSupport.success(redirectAttributes, "profile.payment.refused");
        return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
    }

    private ResponseEntity<byte[]> downloadPaymentProofForUser(final int bookingId, final User user) {
        if (!bookingRequestService.canAccessPaymentProof(bookingId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final BookingPaymentProof proof =
                bookingRequestService.findPaymentProofByBookingId(bookingId).orElse(null);
        if (proof == null || proof.getFileData() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        final String contentType =
                proof.getContentType() == null || proof.getContentType().isBlank()
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                        : proof.getContentType();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + proof.getFileName().replace("\"", "") + "\"")
                .contentLength(proof.getFileData().length)
                .body(proof.getFileData());
    }

    private ModelAndView confirmPaymentReceivedForUser(
            final int bookingId, final User user, final RedirectAttributes redirectAttributes) {
        final BookingRequestService.PaymentConfirmationOutcome outcome =
                bookingRequestService.confirmPaymentReceivedInAccount(bookingId, user.getId());
        if (outcome != BookingRequestService.PaymentConfirmationOutcome.CONFIRMED) {
            ToastSupport.error(redirectAttributes, "profile.payment.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        ToastSupport.success(redirectAttributes, "profile.payment.paid");
        return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
    }

    private ModelAndView resolveBookingRequestForUser(
            final int bookingId,
            final User user,
            final BookingState bookingState,
            final RedirectAttributes redirectAttributes) {
        final BookingRequestService.BookingResolutionOutcome outcome =
                bookingRequestService.resolveBookingRequestInAccount(bookingId, user.getId(), bookingState);
        if (outcome == BookingRequestService.BookingResolutionOutcome.ERROR) {
            ToastSupport.error(redirectAttributes, "profile.bookings.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        if (outcome == BookingRequestService.BookingResolutionOutcome.ACCEPTED) {
            ToastSupport.success(redirectAttributes, "profile.bookings.accepted");
        } else {
            ToastSupport.warning(redirectAttributes, "profile.bookings.rejected");
        }
        return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
    }

    private Optional<User> currentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName());
    }
}
