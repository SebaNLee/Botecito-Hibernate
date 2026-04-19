package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {

    private final UserService userService;
    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;

    public AuthController(
            final UserService userService,
            final ItemService itemService,
            final BookingRequestService bookingRequestService) {
        this.userService = userService;
        this.itemService = itemService;
        this.bookingRequestService = bookingRequestService;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(
            @RequestParam(value = "error", required = false) final String error,
            @RequestParam(value = "logout", required = false) final String logout,
            @RequestParam(value = "registered", required = false) final String registered,
            @RequestParam(value = "legacyToken", required = false) final String legacyToken) {

        final ModelAndView mav = new ModelAndView("login");
        if (error != null) {
            mav.addObject("loginError", true);
        }
        if (logout != null) {
            mav.addObject("logoutSuccess", true);
        }
        if (registered != null) {
            mav.addObject("registeredSuccess", true);
        }
        if (legacyToken != null) {
            mav.addObject("legacyTokenError", true);
        }
        return mav;
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView registerForm(@ModelAttribute("registerForm") final RegisterForm form) {
        return new ModelAndView("register");
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerSubmit(
            @Valid @ModelAttribute("registerForm") final RegisterForm form, final BindingResult errors) {

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "register.validation.password.mismatch");
        }

        if (errors.hasErrors()) {
            return new ModelAndView("register");
        }

        final User existingUser =
                userService.findByEmail(form.getEmail().trim()).orElse(null);
        if (existingUser != null && existingUser.getPasswordHash() != null) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView("register");
        }

        try {
            userService.register(
                    form.getGivenName().trim(),
                    form.getLastName().trim(),
                    form.getEmail().trim(),
                    form.getPassword());
        } catch (final IllegalArgumentException exception) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView("register");
        }

        return new ModelAndView("redirect:/login?registered=true");
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ModelAndView profile() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new ModelAndView("redirect:/login");
        }

        final User user = userService.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("ownedItems", itemService.listItemsByOwnerId(user.getId()));
        mav.addObject("receivedBookingRequests", buildReceivedBookings(user.getId()));
        mav.addObject("sentBookingRequests", buildSentBookings(user.getId()));
        return mav;
    }

    @RequestMapping("/403")
    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }

    private List<ReceivedBookingView> buildReceivedBookings(final int ownerId) {
        final List<ReceivedBookingView> receivedBookings = new ArrayList<>();
        for (final ItemBooking booking : itemService.listBookingsByOwnerId(ownerId)) {
            if (booking.getItemId() == null || booking.getId() == null || booking.getGuestId() == null) {
                continue;
            }

            final Item item = itemService.findItemById(booking.getItemId()).orElse(null);
            if (item == null) {
                continue;
            }

            final User requester =
                    itemService.findUserById(booking.getGuestId()).orElse(null);
            final BookingPaymentProof proof = bookingRequestService
                    .findPaymentProofByBookingId(booking.getId())
                    .orElse(null);
            receivedBookings.add(new ReceivedBookingView(
                    booking.getId(),
                    item.getTitle(),
                    requester == null ? "" : requester.getName(),
                    requester == null ? "" : requester.getEmail(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    statusMessageCode(booking.getState()),
                    proof != null,
                    proof == null ? "" : proof.getFileName()));
        }
        return receivedBookings;
    }

    private List<SentBookingView> buildSentBookings(final int guestId) {
        final List<SentBookingView> sentBookings = new ArrayList<>();
        for (final ItemBooking booking : itemService.listBookingsByGuestId(guestId)) {
            if (booking.getItemId() == null || booking.getId() == null) {
                continue;
            }

            final Item item = itemService.findItemById(booking.getItemId()).orElse(null);
            if (item == null) {
                continue;
            }

            final User owner = item.getOwnerId() == null
                    ? null
                    : itemService.findUserById(item.getOwnerId()).orElse(null);
            sentBookings.add(new SentBookingView(
                    booking.getId(),
                    item.getTitle(),
                    owner == null ? "" : owner.getName(),
                    owner == null ? "" : owner.getEmail(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    statusMessageCode(booking.getState())));
        }
        return sentBookings;
    }

    private static String statusMessageCode(final BookingState state) {
        if (state == null) {
            return "profile.sentBookings.status.unknown";
        }
        return switch (state) {
            case BOOKING_PENDING -> "profile.sentBookings.status.pending";
            case BOOKING_CONFIRMED -> "profile.sentBookings.status.confirmed";
            case BOOKING_REJECTED -> "profile.sentBookings.status.rejected";
            case BOOKING_CANCELLED -> "profile.sentBookings.status.cancelled";
            case BOOKING_COMPLETED -> "profile.sentBookings.status.completed";
            case BOOKING_PAYMENT_SUBMITTED -> "profile.sentBookings.status.paymentSubmitted";
            case BOOKING_PAID -> "profile.sentBookings.status.paid";
        };
    }

    public record ReceivedBookingView(
            int id,
            String itemTitle,
            String requesterName,
            String requesterEmail,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String statusMessageCode,
            boolean hasPaymentProof,
            String paymentProofFileName) {

        public int getId() {
            return id;
        }

        public String getItemTitle() {
            return itemTitle;
        }

        public String getRequesterName() {
            return requesterName;
        }

        public String getRequesterEmail() {
            return requesterEmail;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public String getStatusMessageCode() {
            return statusMessageCode;
        }

        public boolean isHasPaymentProof() {
            return hasPaymentProof;
        }

        public boolean getHasPaymentProof() {
            return hasPaymentProof;
        }

        public String getPaymentProofFileName() {
            return paymentProofFileName;
        }
    }

    public record SentBookingView(
            int id,
            String itemTitle,
            String ownerName,
            String ownerEmail,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String statusMessageCode) {

        public int getId() {
            return id;
        }

        public String getItemTitle() {
            return itemTitle;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public String getOwnerEmail() {
            return ownerEmail;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public String getStatusMessageCode() {
            return statusMessageCode;
        }
    }
}
