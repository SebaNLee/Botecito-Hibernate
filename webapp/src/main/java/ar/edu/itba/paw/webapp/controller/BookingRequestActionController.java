package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PaymentProofForm;
import ar.edu.itba.paw.webapp.form.RefusePaymentForm;
import java.io.IOException;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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

@Controller
public class BookingRequestActionController {

    private static final Set<String> PAYMENT_PROOF_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png", "image/webp");

    private final BookingRequestService bookingRequestService;
    private final ItemService itemService;
    private final MailService mailService;
    private final UserService userService;

    public BookingRequestActionController(
            final BookingRequestService bookingRequestService,
            final ItemService itemService,
            final MailService mailService,
            final UserService userService) {
        this.bookingRequestService = bookingRequestService;
        this.itemService = itemService;
        this.mailService = mailService;
        this.userService = userService;
    }

    @RequestMapping(value = "/bookings/{token}/accept", method = RequestMethod.GET)
    public ModelAndView acceptBookingRequest(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/bookings/{token}/decline", method = RequestMethod.GET)
    public ModelAndView declineBookingRequest(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/accept", method = RequestMethod.POST)
    public ModelAndView acceptBookingRequestInAccount(@PathVariable("id") final int bookingId) {
        return resolveBookingRequestInAccount(bookingId, BookingState.BOOKING_CONFIRMED);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/decline", method = RequestMethod.POST)
    public ModelAndView declineBookingRequestInAccount(@PathVariable("id") final int bookingId) {
        return resolveBookingRequestInAccount(bookingId, BookingState.BOOKING_REJECTED);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment-proof", method = RequestMethod.POST)
    public ModelAndView submitPaymentProof(
            @PathVariable("id") final int bookingId, @ModelAttribute("paymentProofForm") final PaymentProofForm form) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final ItemBooking booking = findBookingById(bookingId);
        if (booking == null
                || booking.getGuestId() == null
                || !booking.getGuestId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/dashboard?paymentAction=forbidden#sent-booking-requests");
        }

        final MultipartFile file = form.getFile();
        if (!isValidPaymentProof(file)) {
            return new ModelAndView("redirect:/dashboard?paymentAction=invalidFile#sent-booking-requests");
        }

        final boolean isResubmit = booking.getState() == BookingState.BOOKING_PAYMENT_REFUSED;
        try {
            final var proof = bookingRequestService.submitPaymentProof(
                    bookingId,
                    currentUser.getId(),
                    cleanFileName(file.getOriginalFilename()),
                    file.getContentType(),
                    file.getBytes(),
                    form.getGuestReply());
            if (proof.isEmpty()) {
                return new ModelAndView("redirect:/dashboard?paymentAction=submitError#sent-booking-requests");
            }

            final Item item = booking.getItemId() == null
                    ? null
                    : itemService.findAnyItemById(booking.getItemId()).orElse(null);
            final User owner = item == null || item.getOwnerId() == null
                    ? null
                    : itemService.findUserById(item.getOwnerId()).orElse(null);
            if (item != null && owner != null) {
                mailService.sendPaymentProofSubmittedEmail(
                        owner.getEmail(),
                        currentUser.getName(),
                        item.getTitle(),
                        proof.get().getFileData(),
                        proof.get().getContentType());
            }
            final String action = isResubmit ? "resubmitted" : "submitted";
            return new ModelAndView("redirect:/profile/dashboard?paymentAction=" + action + "#sent-booking-requests");
        } catch (final IOException e) {
            return new ModelAndView("redirect:/dashboard?paymentAction=submitError#sent-booking-requests");
        }
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/payment/refuse", method = RequestMethod.POST)
    public ModelAndView refusePaymentProof(
            @PathVariable("id") final int bookingId,
            @Valid @ModelAttribute("refusePaymentForm") final RefusePaymentForm form,
            final BindingResult errors) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        if (errors.hasErrors()) {
            return new ModelAndView("redirect:/profile/dashboard?paymentAction=refuseError#received-booking-requests");
        }

        final var refused = bookingRequestService.refusePaymentProof(bookingId, currentUser.getId(), form.getReason());
        if (refused.isEmpty()) {
            return new ModelAndView("redirect:/profile/dashboard?paymentAction=refuseError#received-booking-requests");
        }

        final Item item = refused.get().getItemId() == null
                ? null
                : itemService.findAnyItemById(refused.get().getItemId()).orElse(null);
        mailService.sendPaymentProofRefusedEmail(
                refused.get().getRequesterEmail(),
                refused.get().getRequesterLocaleTag(),
                currentUser.getName(),
                item == null ? "" : item.getTitle(),
                form.getReason());
        return new ModelAndView("redirect:/profile/dashboard?paymentAction=refused#received-booking-requests");
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
    public ModelAndView confirmPaymentReceived(@PathVariable("id") final int bookingId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final var resolved = bookingRequestService.confirmPaymentReceived(bookingId, currentUser.getId());
        if (resolved.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?paymentAction=confirmError#received-booking-requests");
        }

        final Item item = resolved.get().getItemId() == null
                ? null
                : itemService.findAnyItemById(resolved.get().getItemId()).orElse(null);
        mailService.sendPaymentReceivedEmail(
                resolved.get().getRequesterEmail(),
                resolved.get().getRequesterLocaleTag(),
                item == null ? "" : item.getTitle());
        return new ModelAndView("redirect:/dashboard?paymentAction=paid#received-booking-requests");
    }

    private ModelAndView resolveBookingRequestInAccount(final int bookingId, final BookingState bookingState) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final var booking = itemService.listBookings().stream()
                .filter(existingBooking -> existingBooking.getId() != null && existingBooking.getId() == bookingId)
                .findFirst()
                .orElse(null);
        if (booking == null || booking.getHostDecisionToken() == null) {
            return new ModelAndView("redirect:/dashboard?bookingAction=notFound#received-booking-requests");
        }

        final var item = itemService.findAnyItemById(booking.getItemId()).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/dashboard?bookingAction=forbidden#received-booking-requests");
        }

        final var resolved = bookingRequestService.resolveBookingRequest(booking.getHostDecisionToken(), bookingState);
        if (resolved.isEmpty()) {
            return new ModelAndView("redirect:/dashboard?bookingAction=error#received-booking-requests");
        }

        mailService.sendBookingResolutionEmail(resolved.get());
        final String action = bookingState == BookingState.BOOKING_CONFIRMED ? "accepted" : "rejected";
        return new ModelAndView("redirect:/dashboard?bookingAction=" + action + "#received-booking-requests");
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

    private static boolean isValidPaymentProof(final MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 5242880) {
            return false;
        }
        final String contentType = file.getContentType();
        return contentType != null && PAYMENT_PROOF_CONTENT_TYPES.contains(contentType.toLowerCase());
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
