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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
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
        mav.addObject("receivedBookingRequests", buildReceivedBookings(user));
        mav.addObject("sentBookingRequests", buildSentBookings(user.getId()));
        return mav;
    }

    @RequestMapping("/403")
    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }

    private List<ReceivedBookingView> buildReceivedBookings(final User owner) {
        final List<ReceivedBookingView> receivedBookings = new ArrayList<>();
        if (owner == null || owner.getId() == null) {
            return receivedBookings;
        }

        for (final ItemBooking booking : itemService.listBookingsByOwnerId(owner.getId())) {
            if (booking.getItemId() == null || booking.getId() == null || booking.getGuestId() == null) {
                continue;
            }

            final Item item = itemService.findAnyItemById(booking.getItemId()).orElse(null);
            if (item == null) {
                continue;
            }

            final User requester =
                    itemService.findUserById(booking.getGuestId()).orElse(null);
            receivedBookings.add(new ReceivedBookingView(
                    booking.getId(),
                    item.getTitle(),
                    requester == null ? "" : requester.getName(),
                    requester == null ? "" : requester.getEmail(),
                    booking.getStartTime(),
                    booking.getEndTime(),
                    formatDateLabel(booking.getStartTime()),
                    formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()),
                    formatTotalPriceLabel(booking.getStartTime(), booking.getEndTime(), item.getPricePerHour()),
                    resolvePaymentAlias(owner),
                    statusMessageCode(booking.getState()),
                    bookingRequestService
                            .findPaymentProofByBookingId(booking.getId())
                            .map(BookingPaymentProof::getFileName)
                            .orElse("")));
        }
        return receivedBookings;
    }

    private List<SentBookingView> buildSentBookings(final int guestId) {
        final List<SentBookingView> sentBookings = new ArrayList<>();
        for (final ItemBooking booking : itemService.listBookingsByGuestId(guestId)) {
            if (booking.getItemId() == null || booking.getId() == null) {
                continue;
            }

            final Item item = itemService.findAnyItemById(booking.getItemId()).orElse(null);
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
                    formatDateLabel(booking.getStartTime()),
                    formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()),
                    formatTotalPriceLabel(booking.getStartTime(), booking.getEndTime(), item.getPricePerHour()),
                    resolvePaymentAlias(owner),
                    statusMessageCode(booking.getState())));
        }
        return sentBookings;
    }

    private static String formatDateLabel(final OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        final Locale locale = LocaleContextHolder.getLocale();
        return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));
    }

    private static String formatTimeRangeLabel(final OffsetDateTime startTime, final OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return startTime.format(formatter) + " - " + endTime.format(formatter);
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

    private static String formatTotalPriceLabel(
            final OffsetDateTime startTime, final OffsetDateTime endTime, final Integer pricePerHour) {
        if (startTime == null || endTime == null || pricePerHour == null || pricePerHour < 0) {
            return "";
        }

        final long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return "";
        }

        final BigDecimal totalPrice = BigDecimal.valueOf(pricePerHour.longValue())
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(LocaleContextHolder.getLocale());
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(totalPrice);
    }

    private static String resolvePaymentAlias(final User user) {
        if (user == null) {
            return "";
        }
        if (user.getPaymentAlias() != null && !user.getPaymentAlias().isBlank()) {
            return user.getPaymentAlias();
        }
        return user.getEmail() == null ? "" : user.getEmail();
    }

    public record ReceivedBookingView(
            int id,
            String itemTitle,
            String requesterName,
            String requesterEmail,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String dateLabel,
            String timeRangeLabel,
            String totalPriceLabel,
            String paymentAlias,
            String statusMessageCode,
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

        public String getDateLabel() {
            return dateLabel;
        }

        public String getTimeRangeLabel() {
            return timeRangeLabel;
        }

        public String getTotalPriceLabel() {
            return totalPriceLabel;
        }

        public String getPaymentAlias() {
            return paymentAlias;
        }

        public String getStatusMessageCode() {
            return statusMessageCode;
        }

        public boolean isHasPaymentProof() {
            return paymentProofFileName != null && !paymentProofFileName.isBlank();
        }

        public boolean getHasPaymentProof() {
            return isHasPaymentProof();
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
            String dateLabel,
            String timeRangeLabel,
            String totalPriceLabel,
            String paymentAlias,
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

        public String getDateLabel() {
            return dateLabel;
        }

        public String getTimeRangeLabel() {
            return timeRangeLabel;
        }

        public String getTotalPriceLabel() {
            return totalPriceLabel;
        }

        public String getPaymentAlias() {
            return paymentAlias;
        }

        public String getStatusMessageCode() {
            return statusMessageCode;
        }
    }
}
