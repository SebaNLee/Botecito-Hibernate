package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {

    private final UserService userService;
    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;
    private final MailService mailService;
    private final ReviewService reviewService;
    private final AuthenticationManager authenticationManager;

    public AuthController(
            final UserService userService,
            final ItemService itemService,
            final BookingRequestService bookingRequestService,
            final MailService mailService,
            final ReviewService reviewService,
            final AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.itemService = itemService;
        this.bookingRequestService = bookingRequestService;
        this.mailService = mailService;
        this.reviewService = reviewService;
        this.authenticationManager = authenticationManager;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(
            @RequestParam(value = "error", required = false) final String error,
            @RequestParam(value = "logout", required = false) final String logout,
            @RequestParam(value = "registered", required = false) final String registered,
            @RequestParam(value = "legacyToken", required = false) final String legacyToken,
            @RequestParam(value = "passwordRecovered", required = false) final String passwordRecovered) {

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
        if (passwordRecovered != null) {
            mav.addObject("passwordRecoveredSuccess", true);
        }
        return mav;
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView registerForm(@ModelAttribute("registerForm") final RegisterForm form) {
        return new ModelAndView("register");
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerSubmit(
            @Valid @ModelAttribute("registerForm") final RegisterForm form,
            final BindingResult errors,
            final HttpServletRequest request) {

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
                    form.getPassword(),
                    form.getPaymentAlias());
        } catch (final IllegalArgumentException exception) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView("register");
        }

        if (!authenticateRegisteredUser(form.getEmail().trim(), form.getPassword(), request)) {
            return new ModelAndView("redirect:/login?registered=true");
        }
        return new ModelAndView("redirect:/");
    }

    private boolean authenticateRegisteredUser(
            final String email, final String rawPassword, final HttpServletRequest request) {
        final UsernamePasswordAuthenticationToken requestToken =
                new UsernamePasswordAuthenticationToken(email, rawPassword);
        requestToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(requestToken);
        } catch (final AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            final HttpSession existingSession = request.getSession(false);
            if (existingSession != null) {
                existingSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            }
            return false;
        }

        final SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        final HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return true;
    }

    @RequestMapping(value = "/password-recovery", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryRequestForm(
            @ModelAttribute("passwordRecoveryRequestForm") final PasswordRecoveryRequestForm form,
            @RequestParam(value = "sent", required = false) final String sent) {
        final ModelAndView mav = new ModelAndView("password-recovery-request");
        if (sent != null) {
            mav.addObject("recoverySent", true);
        }
        return mav;
    }

    @RequestMapping(value = "/password-recovery", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryRequestSubmit(
            @Valid @ModelAttribute("passwordRecoveryRequestForm") final PasswordRecoveryRequestForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("password-recovery-request");
        }

        userService
                .requestPasswordRecovery(form.getEmail().trim())
                .ifPresent(user -> mailService.sendPasswordRecoveryEmail(
                        user.getEmail(),
                        user.getName().isBlank() ? user.getEmail() : user.getName(),
                        user.getPasswordRecoveryToken()));

        return new ModelAndView("redirect:/password-recovery?sent=true");
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.GET)
    public ModelAndView passwordRecoveryResetForm(
            @PathVariable("token") final String token,
            @ModelAttribute("passwordResetForm") final PasswordResetForm form,
            @RequestParam(value = "invalid", required = false) final String invalid) {
        final ModelAndView mav = new ModelAndView("password-recovery-reset");
        mav.addObject("token", token);
        mav.addObject(
                "tokenValid", userService.findByPasswordRecoveryToken(token).isPresent());
        if (invalid != null) {
            mav.addObject("tokenInvalidError", true);
        }
        return mav;
    }

    @RequestMapping(value = "/password-recovery/{token}", method = RequestMethod.POST)
    public ModelAndView passwordRecoveryResetSubmit(
            @PathVariable("token") final String token,
            @Valid @ModelAttribute("passwordResetForm") final PasswordResetForm form,
            final BindingResult errors) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "passwordRecovery.reset.validation.password.mismatch");
        }

        final boolean tokenValid =
                userService.findByPasswordRecoveryToken(token).isPresent();
        if (!tokenValid) {
            return new ModelAndView("password-recovery-reset")
                    .addObject("token", token)
                    .addObject("tokenValid", false);
        }

        if (errors.hasErrors()) {
            return new ModelAndView("password-recovery-reset")
                    .addObject("token", token)
                    .addObject("tokenValid", true);
        }

        if (userService.resetPassword(token, form.getPassword()) != UserService.PasswordRecoveryResult.SUCCESS) {
            return new ModelAndView("redirect:/password-recovery/" + token + "?invalid=true");
        }

        return new ModelAndView("redirect:/login?passwordRecovered=true");
    }

    @RequestMapping(value = "/profile/password-recovery", method = RequestMethod.POST)
    public ModelAndView profilePasswordRecoveryRequest() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new ModelAndView("redirect:/login");
        }

        userService.findByEmail(authentication.getName()).ifPresent(user -> userService
                .requestPasswordRecovery(user.getEmail())
                .ifPresent(updatedUser -> mailService.sendPasswordRecoveryEmail(
                        updatedUser.getEmail(),
                        updatedUser.getName().isBlank() ? updatedUser.getEmail() : updatedUser.getName(),
                        updatedUser.getPasswordRecoveryToken())));

        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ModelAndView profile(@ModelAttribute("profileForm") final ProfileForm form) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        populateProfileForm(form, user);
        return buildProfileView(user);
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public ModelAndView profileSubmit(
            @Valid @ModelAttribute("profileForm") final ProfileForm form, final BindingResult errors) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser);
        }

        final User updatedUser = userService
                .updateProfile(
                        currentUser.getId(),
                        form.getGivenName(),
                        form.getLastName(),
                        form.getEmail(),
                        form.getPhone(),
                        form.getPaymentAlias(),
                        form.getPreferredLanguage())
                .orElse(null);
        if (updatedUser == null) {
            errors.rejectValue("email", "profile.validation.email.duplicate");
            return buildProfileView(currentUser);
        }

        refreshAuthenticatedPrincipal(updatedUser);
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    @RequestMapping(value = "/profile/dashboard", method = RequestMethod.GET)
    public ModelAndView dashboard(@RequestParam(value = "dashboardTab", required = false) final String dashboardTab) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        final ModelAndView mav = new ModelAndView("dashboard");
        mav.addObject("user", user);
        mav.addObject("activeDashboardTab", resolveDashboardTab(dashboardTab));
        addDashboardData(mav, user);
        return mav;
    }

    private ModelAndView buildProfileView(final User user) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("memberSinceDisplay", formatMemberSince(user.getCreatedAt()));
        return mav;
    }

    private void addDashboardData(final ModelAndView mav, final User user) {
        final List<Item> ownedItems = itemService.listItemsByOwnerId(user.getId());
        final List<PendingReviewView> pendingReviewActions = buildPendingReviewActions(user.getId());
        final Map<Integer, PendingReviewView> pendingGuestItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, PendingReviewView> pendingOwnerUserReviewsByBookingId = new LinkedHashMap<>();
        for (final PendingReviewView pendingReview : pendingReviewActions) {
            if (pendingReview.targetType() == ReviewTargetType.ITEM) {
                pendingGuestItemReviewsByBookingId.put(pendingReview.bookingId(), pendingReview);
            } else if (pendingReview.targetType() == ReviewTargetType.USER) {
                pendingOwnerUserReviewsByBookingId.put(pendingReview.bookingId(), pendingReview);
            }
        }

        mav.addObject("ownedItems", ownedItems);
        mav.addObject("receivedBookingRequests", buildReceivedBookings(user));
        mav.addObject("sentBookingRequests", buildSentBookings(user.getId()));
        mav.addObject("pendingGuestItemReviewsByBookingId", pendingGuestItemReviewsByBookingId);
        mav.addObject("pendingOwnerUserReviewsByBookingId", pendingOwnerUserReviewsByBookingId);
        mav.addObject("authoredItemReviewsByBookingId", buildAuthoredItemReviewsByBookingId(user.getId()));
        mav.addObject("authoredUserReviewsByBookingId", buildAuthoredUserReviewsByBookingId(user.getId()));
        mav.addObject("receivedGuestReviews", buildReceivedUserReviews(user.getId()));
        mav.addObject(
                "receivedItemReviewsByOwnedItems", buildReceivedItemReviewsByOwnedItems(user.getId(), ownedItems));
    }

    private static void populateProfileForm(final ProfileForm form, final User user) {
        if (form == null || user == null || form.getEmail() != null) {
            return;
        }

        form.setGivenName(user.getGivenName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setPaymentAlias(user.getPaymentAlias());
        form.setPreferredLanguage(user.getPreferredLanguage());
    }

    private static String resolveDashboardTab(final String requestedTab) {
        if ("bookings".equals(requestedTab) || "reviews".equals(requestedTab)) {
            return requestedTab;
        }
        return "hosting";
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

    private static void refreshAuthenticatedPrincipal(final User user) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || user == null || user.getEmail() == null) {
            return;
        }

        final UsernamePasswordAuthenticationToken refreshed = new UsernamePasswordAuthenticationToken(
                user.getEmail(), authentication.getCredentials(), authentication.getAuthorities());
        refreshed.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(refreshed);
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

        final Map<Integer, RatingSummary> requesterRatingByUserId = new LinkedHashMap<>();

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
            final RatingSummary requesterRating = requesterRatingByUserId.computeIfAbsent(
                    booking.getGuestId(), guestId -> reviewService.listReceivedReviews(guestId).stream()
                            .filter(review -> review.getTargetType() == ReviewTargetType.USER)
                            .collect(java.util.stream.Collectors.collectingAndThen(
                                    java.util.stream.Collectors.toList(), reviews -> {
                                        if (reviews.isEmpty()) {
                                            return new RatingSummary();
                                        }
                                        final double average = reviews.stream()
                                                .map(Review::getRating)
                                                .filter(java.util.Objects::nonNull)
                                                .mapToInt(Integer::intValue)
                                                .average()
                                                .orElse(0.0);
                                        return new RatingSummary(average, reviews.size());
                                    })));
            receivedBookings.add(new ReceivedBookingView(
                    booking.getId(),
                    item.getTitle(),
                    requester == null ? "" : requester.getName(),
                    requester == null ? "" : requester.getEmail(),
                    requesterRating.getAverageRating(),
                    requesterRating.getTotalReviews(),
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

    private List<PendingReviewView> buildPendingReviewActions(final int userId) {
        final List<PendingReviewView> pendingReviews = new ArrayList<>();
        for (final ReviewService.PendingReviewAction action : reviewService.listPendingReviewActions(userId)) {
            final Item item = itemService.findAnyItemById(action.getItemId()).orElse(null);
            final User targetUser =
                    itemService.findUserById(action.getTargetUserId()).orElse(null);
            if (item == null || targetUser == null) {
                continue;
            }

            pendingReviews.add(new PendingReviewView(
                    action.getBookingId(),
                    item.getId(),
                    item.getTitle(),
                    action.getTargetType(),
                    targetUser.getName(),
                    targetUser.getEmail(),
                    formatDateLabel(action.getStartTime()),
                    formatTimeRangeLabel(action.getStartTime(), action.getEndTime())));
        }
        return pendingReviews;
    }

    private List<AuthoredReviewView> buildAuthoredReviews(final int reviewerUserId) {
        final List<AuthoredReviewView> authoredReviews = new ArrayList<>();
        for (final Review review : reviewService.listAuthoredReviews(reviewerUserId)) {
            if (review.getTargetType() == null || review.getTargetId() == null) {
                continue;
            }

            final String contextTitle = resolveReviewContextTitle(review.getTargetType(), review.getTargetId());
            final User reviewee = review.getRevieweeUserId() == null
                    ? null
                    : itemService.findUserById(review.getRevieweeUserId()).orElse(null);
            authoredReviews.add(new AuthoredReviewView(
                    review.getId(),
                    review.getBookingId() == null ? 0 : review.getBookingId(),
                    review.getTargetType(),
                    contextTitle,
                    reviewee == null ? "" : reviewee.getName(),
                    reviewee == null ? "" : reviewee.getEmail(),
                    review.getRating() == null ? 0 : review.getRating(),
                    review.getComment(),
                    formatDateLabel(review.getCreatedAt())));
        }
        return authoredReviews;
    }

    private Map<Integer, AuthoredItemReviewSummaryView> buildAuthoredItemReviewsByBookingId(final int reviewerUserId) {
        final Map<Integer, AuthoredItemReviewSummaryView> reviewsByBookingId = new LinkedHashMap<>();
        for (final Review review : reviewService.listAuthoredReviews(reviewerUserId)) {
            if (review.getBookingId() == null || review.getTargetType() != ReviewTargetType.ITEM) {
                continue;
            }
            reviewsByBookingId.put(
                    review.getBookingId(),
                    new AuthoredItemReviewSummaryView(
                            review.getRating() == null ? 0 : review.getRating(),
                            review.getComment() == null ? "" : review.getComment()));
        }
        return reviewsByBookingId;
    }

    private Map<Integer, AuthoredItemReviewSummaryView> buildAuthoredUserReviewsByBookingId(final int reviewerUserId) {
        final Map<Integer, AuthoredItemReviewSummaryView> reviewsByBookingId = new LinkedHashMap<>();
        for (final Review review : reviewService.listAuthoredReviews(reviewerUserId)) {
            if (review.getBookingId() == null || review.getTargetType() != ReviewTargetType.USER) {
                continue;
            }
            reviewsByBookingId.put(
                    review.getBookingId(),
                    new AuthoredItemReviewSummaryView(
                            review.getRating() == null ? 0 : review.getRating(),
                            review.getComment() == null ? "" : review.getComment()));
        }
        return reviewsByBookingId;
    }

    private List<ReceivedReviewView> buildReceivedReviews(final int revieweeUserId) {
        final List<ReceivedReviewView> receivedReviews = new ArrayList<>();
        for (final Review review : reviewService.listReceivedReviews(revieweeUserId)) {
            if (review.getTargetType() == null || review.getTargetId() == null) {
                continue;
            }

            final User reviewer = review.getReviewerUserId() == null
                    ? null
                    : itemService.findUserById(review.getReviewerUserId()).orElse(null);
            final String contextTitle = resolveReviewContextTitle(review.getTargetType(), review.getTargetId());
            receivedReviews.add(new ReceivedReviewView(
                    review.getTargetType(),
                    contextTitle,
                    reviewer == null ? "" : reviewer.getName(),
                    reviewer == null ? "" : reviewer.getEmail(),
                    review.getRating() == null ? 0 : review.getRating(),
                    review.getComment(),
                    formatDateLabel(review.getCreatedAt())));
        }
        return receivedReviews;
    }

    private List<ReceivedReviewView> buildReceivedUserReviews(final int revieweeUserId) {
        final List<ReceivedReviewView> receivedReviews = new ArrayList<>();
        for (final ReceivedReviewView review : buildReceivedReviews(revieweeUserId)) {
            if (review.targetType() == ReviewTargetType.USER) {
                receivedReviews.add(review);
            }
        }
        return receivedReviews;
    }

    private List<ItemReviewGroupView> buildReceivedItemReviewsByOwnedItems(
            final int ownerUserId, final List<Item> ownedItems) {
        final Map<Integer, String> itemTitlesById = new LinkedHashMap<>();
        if (ownedItems != null) {
            for (final Item item : ownedItems) {
                if (item == null || item.getId() == null) {
                    continue;
                }
                itemTitlesById.put(item.getId(), item.getTitle() == null ? "" : item.getTitle());
            }
        }

        final Map<Integer, List<ReceivedReviewView>> reviewsByItemId = new LinkedHashMap<>();
        for (final Review review : reviewService.listReceivedReviews(ownerUserId)) {
            if (review.getTargetType() != ReviewTargetType.ITEM || review.getTargetId() == null) {
                continue;
            }
            if (!itemTitlesById.containsKey(review.getTargetId())) {
                continue;
            }

            final User reviewer = review.getReviewerUserId() == null
                    ? null
                    : itemService.findUserById(review.getReviewerUserId()).orElse(null);
            final ReceivedReviewView reviewView = new ReceivedReviewView(
                    review.getTargetType(),
                    itemTitlesById.get(review.getTargetId()),
                    reviewer == null ? "" : reviewer.getName(),
                    reviewer == null ? "" : reviewer.getEmail(),
                    review.getRating() == null ? 0 : review.getRating(),
                    review.getComment(),
                    formatDateLabel(review.getCreatedAt()));
            reviewsByItemId
                    .computeIfAbsent(review.getTargetId(), ignored -> new ArrayList<>())
                    .add(reviewView);
        }

        final List<ItemReviewGroupView> groups = new ArrayList<>();
        for (final Map.Entry<Integer, String> itemEntry : itemTitlesById.entrySet()) {
            final List<ReceivedReviewView> itemReviews = reviewsByItemId.get(itemEntry.getKey());
            if (itemReviews == null || itemReviews.isEmpty()) {
                continue;
            }
            groups.add(new ItemReviewGroupView(itemEntry.getKey(), itemEntry.getValue(), itemReviews));
        }
        return groups;
    }

    private String resolveReviewContextTitle(final ReviewTargetType targetType, final int targetId) {
        if (targetType == ReviewTargetType.ITEM) {
            return itemService.findAnyItemById(targetId).map(Item::getTitle).orElse("");
        }
        return itemService.findUserById(targetId).map(User::getName).orElse("");
    }

    private static String formatDateLabel(final OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        final Locale locale = LocaleContextHolder.getLocale();
        return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));
    }

    private static String formatMemberSince(final OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String formatTimeRangeLabel(final OffsetDateTime startTime, final OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return startTime.format(formatter) + " hs - " + endTime.format(formatter) + " hs";
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
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
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
            double requesterAverageRating,
            int requesterTotalReviews,
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

        public double getRequesterAverageRating() {
            return requesterAverageRating;
        }

        public int getRequesterTotalReviews() {
            return requesterTotalReviews;
        }

        public boolean isRequesterHasReviews() {
            return requesterTotalReviews > 0;
        }

        public boolean getRequesterHasReviews() {
            return isRequesterHasReviews();
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

    public record PendingReviewView(
            int bookingId,
            int itemId,
            String itemTitle,
            ReviewTargetType targetType,
            String targetName,
            String targetEmail,
            String dateLabel,
            String timeRangeLabel) {

        public int getBookingId() {
            return bookingId;
        }

        public int getItemId() {
            return itemId;
        }

        public String getItemTitle() {
            return itemTitle;
        }

        public ReviewTargetType getTargetType() {
            return targetType;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getTargetEmail() {
            return targetEmail;
        }

        public String getDateLabel() {
            return dateLabel;
        }

        public String getTimeRangeLabel() {
            return timeRangeLabel;
        }
    }

    public record AuthoredReviewView(
            int reviewId,
            int bookingId,
            ReviewTargetType targetType,
            String contextTitle,
            String revieweeName,
            String revieweeEmail,
            int rating,
            String comment,
            String createdAtLabel) {

        public int getReviewId() {
            return reviewId;
        }

        public int getBookingId() {
            return bookingId;
        }

        public ReviewTargetType getTargetType() {
            return targetType;
        }

        public String getContextTitle() {
            return contextTitle;
        }

        public String getRevieweeName() {
            return revieweeName;
        }

        public String getRevieweeEmail() {
            return revieweeEmail;
        }

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }

        public String getCreatedAtLabel() {
            return createdAtLabel;
        }
    }

    public record ReceivedReviewView(
            ReviewTargetType targetType,
            String contextTitle,
            String reviewerName,
            String reviewerEmail,
            int rating,
            String comment,
            String createdAtLabel) {

        public ReviewTargetType getTargetType() {
            return targetType;
        }

        public String getContextTitle() {
            return contextTitle;
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public String getReviewerEmail() {
            return reviewerEmail;
        }

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }

        public String getCreatedAtLabel() {
            return createdAtLabel;
        }
    }

    public static final class ItemReviewGroupView {
        private final int itemId;
        private final String itemTitle;
        private final List<ReceivedReviewView> reviews;

        public ItemReviewGroupView(final int itemId, final String itemTitle, final List<ReceivedReviewView> reviews) {
            this.itemId = itemId;
            this.itemTitle = itemTitle;
            this.reviews = reviews == null ? List.of() : List.copyOf(reviews);
        }

        public int getItemId() {
            return itemId;
        }

        public String getItemTitle() {
            return itemTitle;
        }

        public List<ReceivedReviewView> getReviews() {
            return reviews;
        }
    }

    public record AuthoredItemReviewSummaryView(int rating, String comment) {

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }
    }
}
