package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.dto.PaymentProofUpload;
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
    private final ItemService itemService;
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
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final MultipartFile file = form.getFile();
        final byte[] fileBytes;
        try {
            fileBytes = file == null || file.isEmpty() ? new byte[0] : file.getBytes();
        } catch (final IOException e) {
            ToastSupport.error(redirectAttributes, "profile.payment.invalidFile");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }

        final PaymentProofUpload upload = new PaymentProofUpload(
                cleanFileName(file == null ? null : file.getOriginalFilename()),
                file == null ? null : file.getContentType(),
                fileBytes,
                form.getGuestReply());

        final var existingProof = bookingRequestService.findPaymentProofByBookingId(bookingId);

        try {
            final var proof = bookingRequestService.submitPaymentProof(bookingId, currentUser.getId(), upload);
            if (proof.isEmpty()) {
                ToastSupport.error(redirectAttributes, "profile.payment.invalidFile");
                return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
            }
            ToastSupport.success(
                    redirectAttributes,
                    existingProof.isPresent() ? "profile.payment.resubmitted" : "profile.payment.submitted");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        } catch (final RuntimeException e) {
            ToastSupport.error(redirectAttributes, "profile.payment.error");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }
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
        try {
            final var refused =
                    bookingRequestService.refusePaymentProof(bookingId, currentUser.getId(), form.getReason());
            if (refused.isEmpty()) {
                ToastSupport.error(redirectAttributes, "profile.payment.refuseError");
                return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
            }
            ToastSupport.success(redirectAttributes, "profile.payment.refused");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        } catch (final RuntimeException e) {
            ToastSupport.error(redirectAttributes, "profile.payment.refuseError");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
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
        try {
            final var resolved = bookingRequestService.confirmPaymentReceived(bookingId, currentUser.getId());
            if (resolved.isEmpty()) {
                ToastSupport.error(redirectAttributes, "profile.payment.error");
                return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
            }
            ToastSupport.success(redirectAttributes, "profile.payment.paid");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        } catch (final RuntimeException e) {
            ToastSupport.error(redirectAttributes, "profile.payment.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
    }

    private ModelAndView resolveBookingRequestInAccount(
            final int bookingId, final BookingState bookingState, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final var booking = itemService.listBookings().stream()
                .filter(existingBooking -> existingBooking.getId() != null && existingBooking.getId() == bookingId)
                .findFirst()
                .orElse(null);
        if (booking == null || booking.getHostDecisionToken() == null) {
            ToastSupport.error(redirectAttributes, "profile.bookings.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }

        final var item = itemService.findAnyItemById(booking.getItemId()).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            ToastSupport.error(redirectAttributes, "profile.bookings.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }

        try {
            final var resolved =
                    bookingRequestService.resolveBookingRequest(booking.getHostDecisionToken(), bookingState);
            if (resolved.isEmpty()) {
                ToastSupport.error(redirectAttributes, "profile.bookings.error");
                return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
            }
            if (bookingState == BookingState.BOOKING_CONFIRMED) {
                ToastSupport.success(redirectAttributes, "profile.bookings.accepted");
            } else {
                ToastSupport.warning(redirectAttributes, "profile.bookings.rejected");
            }
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        } catch (final RuntimeException e) {
            ToastSupport.error(redirectAttributes, "profile.bookings.error");
            return new ModelAndView(DASHBOARD_HOSTING_REDIRECT);
        }
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
