package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.RefusePaymentForm;
import java.io.IOException;
import java.util.Set;
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

    private static final Set<String> PAYMENT_PROOF_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");
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

        final ItemBooking booking = findBookingById(bookingId);
        if (booking == null
                || booking.getGuestId() == null
                || !booking.getGuestId().equals(currentUser.getId())) {
            ToastSupport.error(redirectAttributes, "profile.payment.error");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }

        final MultipartFile file = form.getFile();
        final byte[] fileBytes;
        try {
            fileBytes = file == null || file.isEmpty() ? new byte[0] : file.getBytes();
        } catch (final IOException e) {
            ToastSupport.error(redirectAttributes, "profile.payment.invalidFile");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }
        if (!isValidPaymentProof(file, fileBytes)) {
            ToastSupport.error(redirectAttributes, "profile.payment.invalidFile");
            return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
        }

        final boolean isResubmit = booking.getState() == BookingState.BOOKING_PAYMENT_REFUSED;
        try {
            final var proof = bookingRequestService.submitPaymentProof(
                    bookingId,
                    currentUser.getId(),
                    cleanFileName(file.getOriginalFilename()),
                    file.getContentType(),
                    fileBytes,
                    form.getGuestReply());
            if (proof.isEmpty()) {
                ToastSupport.error(redirectAttributes, "profile.payment.error");
                return new ModelAndView(DASHBOARD_BOOKINGS_REDIRECT);
            }

            ToastSupport.success(
                    redirectAttributes, isResubmit ? "profile.payment.resubmitted" : "profile.payment.submitted");
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

        final ItemBooking booking = findBookingById(bookingId);
        if (booking == null || !canAccessPaymentProof(booking, currentUser.getId())) {
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

    private ItemBooking findBookingById(final int bookingId) {
        return itemService.listBookings().stream()
                .filter(existingBooking -> existingBooking.getId() != null && existingBooking.getId() == bookingId)
                .findFirst()
                .orElse(null);
    }

    private boolean canAccessPaymentProof(final ItemBooking booking, final int userId) {
        if (booking.getGuestId() != null && booking.getGuestId().equals(userId)) {
            return true;
        }
        if (booking.getItemId() == null) {
            return false;
        }
        final Item item = itemService.findAnyItemById(booking.getItemId()).orElse(null);
        return item != null && item.getOwnerId() != null && item.getOwnerId().equals(userId);
    }

    private static boolean isValidPaymentProof(final MultipartFile file, final byte[] fileBytes) {
        if (file == null || file.isEmpty() || file.getSize() > 5242880) {
            return false;
        }
        final String contentType = file.getContentType();
        if (contentType == null || !PAYMENT_PROOF_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return false;
        }
        return matchesMagicBytes(contentType.toLowerCase(), fileBytes);
    }

    private static boolean matchesMagicBytes(final String contentType, final byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        switch (contentType) {
            case "image/jpeg":
                return (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
            case "image/png":
                return data.length >= 8
                        && (data[0] & 0xFF) == 0x89
                        && data[1] == 'P'
                        && data[2] == 'N'
                        && data[3] == 'G'
                        && (data[4] & 0xFF) == 0x0D
                        && (data[5] & 0xFF) == 0x0A
                        && (data[6] & 0xFF) == 0x1A
                        && (data[7] & 0xFF) == 0x0A;
            case "image/webp":
                return data.length >= 12
                        && data[0] == 'R'
                        && data[1] == 'I'
                        && data[2] == 'F'
                        && data[3] == 'F'
                        && data[8] == 'W'
                        && data[9] == 'E'
                        && data[10] == 'B'
                        && data[11] == 'P';
            case "application/pdf":
                return data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
            default:
                return false;
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
