package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.RefusePaymentForm;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BookingRequestActionController {

    private static final String DASHBOARD_BOOKINGS_REDIRECT = "redirect:/bookings#sent-booking-requests";
    private static final String DASHBOARD_HOSTING_REDIRECT = "redirect:/my-boats#received-booking-requests";

    private final BookingRequestService bookingRequestService;
    private final UserService userService;

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
        return resolveBookingRequestInAccount(bookingId, BookingState.BOOKING_CONFIRMED, redirectAttributes);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/decline", method = RequestMethod.POST)
    public ModelAndView declineBookingRequestInAccount(
            @PathVariable("id") final int bookingId, final RedirectAttributes redirectAttributes) {
        return resolveBookingRequestInAccount(bookingId, BookingState.BOOKING_REJECTED, redirectAttributes);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment-proof", method = RequestMethod.POST)
    public ModelAndView submitPaymentProof(
            @PathVariable("id") final int bookingId,
            @ModelAttribute("paymentProofForm") final PaymentProofForm form,
            final RedirectAttributes redirectAttributes)
            throws IOException {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final MultipartFile file = form.getFile();
        final byte[] fileBytes = file == null || file.isEmpty() ? new byte[0] : file.getBytes();

        final BookingRequestService.PaymentProofSubmissionOutcome outcome =
                bookingRequestService.submitPaymentProofInAccount(
                        bookingId,
                        currentUser.getId(),
                        cleanFileName(file == null ? null : file.getOriginalFilename()),
                        file == null ? null : file.getContentType(),
                        fileBytes,
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

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment/refuse", method = RequestMethod.POST)
    public ModelAndView refusePaymentProof(
            @PathVariable("id") final int bookingId,
            @Valid @ModelAttribute("refusePaymentForm") final RefusePaymentForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        if (errors.hasErrors()) {
            ToastSupport.error(redirectAttributes, "profile.payment.refuseError");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        final BookingRequestService.PaymentRefusalOutcome outcome =
                bookingRequestService.refusePaymentProofInAccount(bookingId, currentUser.getId(), form.getReason());
        if (outcome != BookingRequestService.PaymentRefusalOutcome.REFUSED) {
            ToastSupport.error(redirectAttributes, "profile.payment.refuseError");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        ToastSupport.success(redirectAttributes, "profile.payment.refused");
        return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment-proof", method = RequestMethod.GET)
    public void downloadPaymentProof(@PathVariable("id") final int bookingId, final HttpServletResponse response)
            throws IOException {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!bookingRequestService.canAccessPaymentProof(bookingId, currentUser.getId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final BookingPaymentProof proof =
                bookingRequestService.findPaymentProofByBookingId(bookingId).orElse(null);
        if (proof == null || proof.getFileData() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(proof.getContentType());
        response.setHeader(
                "Content-Disposition",
                "inline; filename=\"" + proof.getFileName().replace("\"", "") + "\"");
        response.setContentLength(proof.getFileData().length);
        response.getOutputStream().write(proof.getFileData());
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment/confirm", method = RequestMethod.POST)
    public ModelAndView confirmPaymentReceived(
            @PathVariable("id") final int bookingId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final BookingRequestService.PaymentConfirmationOutcome outcome =
                bookingRequestService.confirmPaymentReceivedInAccount(bookingId, currentUser.getId());
        if (outcome != BookingRequestService.PaymentConfirmationOutcome.CONFIRMED) {
            ToastSupport.error(redirectAttributes, "profile.payment.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
        ToastSupport.success(redirectAttributes, "profile.payment.paid");
        return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
    }

    private ModelAndView resolveBookingRequestInAccount(
            final int bookingId, final BookingState bookingState, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final BookingRequestService.BookingResolutionOutcome outcome =
                bookingRequestService.resolveBookingRequestInAccount(bookingId, currentUser.getId(), bookingState);
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

    private static String cleanFileName(final String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "comprobante";
        }
        final int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        return slash >= 0 ? fileName.substring(slash + 1) : fileName;
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
