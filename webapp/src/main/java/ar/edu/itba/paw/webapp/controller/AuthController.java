package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.auth.PostRegistrationAuthenticator;
import ar.edu.itba.paw.webapp.form.PasswordRecoveryRequestForm;
import ar.edu.itba.paw.webapp.form.PasswordResetForm;
import ar.edu.itba.paw.webapp.form.ProfileForm;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import ar.edu.itba.paw.webapp.util.BookingDisplayFormatter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;
    private final PostRegistrationAuthenticator postRegistrationAuthenticator;

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

        if (!postRegistrationAuthenticator.authenticate(form.getEmail().trim(), form.getPassword(), request)) {
            return new ModelAndView("redirect:/login?registered=true");
        }
        return new ModelAndView("redirect:/");
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

        userService.requestPasswordRecovery(form.getEmail().trim());

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

        userService
                .findByEmail(authentication.getName())
                .ifPresent(user -> userService.requestPasswordRecovery(user.getEmail()));

        return new ModelAndView("redirect:/profile?passwordRecovery=sent");
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ModelAndView profile(
            @RequestParam(value = "edit", defaultValue = "false") final boolean edit,
            @ModelAttribute("profileForm") final ProfileForm form) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        populateProfileForm(form, user);
        return buildProfileView(user, edit);
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public ModelAndView profileSubmit(
            @Valid @ModelAttribute("profileForm") final ProfileForm form, final BindingResult errors) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (errors.hasErrors()) {
            return buildProfileView(currentUser, true);
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
            return buildProfileView(currentUser, true);
        }

        refreshAuthenticatedPrincipal(updatedUser);
        return new ModelAndView("redirect:/profile?profileAction=updated");
    }

    @RequestMapping(value = "/dashboard", method = RequestMethod.GET)
    public ModelAndView dashboard() {
        return new ModelAndView("redirect:/my-boats");
    }

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(
            @RequestParam(value = "status", required = false) final List<String> status,
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int page,
            final HttpServletRequest request) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        final ModelAndView mav = new ModelAndView("my-boats");
        mav.addObject("user", user);
        addMyBoatsData(
                mav,
                user,
                resolveBookingStatusFilters(status),
                normalizeQuery(query),
                sanitizePage(page),
                request.getContextPath());
        return mav;
    }

    @RequestMapping(value = "/bookings", method = RequestMethod.GET)
    public ModelAndView bookings(
            @RequestParam(value = "status", required = false) final List<String> status,
            @RequestParam(value = "q", required = false) final String query,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int page,
            final HttpServletRequest request) {
        final User user = currentAuthenticatedUser();
        if (user == null) {
            return new ModelAndView("redirect:/login");
        }

        final ModelAndView mav = new ModelAndView("bookings");
        mav.addObject("user", user);
        addMyTripsData(
                mav,
                user,
                resolveBookingStatusFilters(status),
                normalizeQuery(query),
                sanitizePage(page),
                request.getContextPath());
        return mav;
    }

    @RequestMapping(value = "/profile/dashboard", method = RequestMethod.GET)
    public ModelAndView legacyDashboardRedirect() {
        return new ModelAndView("redirect:/my-boats");
    }

    private ModelAndView buildProfileView(final User user, final boolean profileEdit) {
        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        mav.addObject("memberSinceDisplay", formatMemberSince(user.getCreatedAt()));
        mav.addObject("profileEdit", profileEdit);
        return mav;
    }

    private static final int DASHBOARD_PAGE_SIZE = 6;

    private void addMyBoatsData(
            final ModelAndView mav,
            final User user,
            final List<String> statuses,
            final String query,
            final int page,
            final String contextPath) {
        final List<Item> ownedItems = itemService.listItemsByOwnerId(user.getId());
        final Map<Integer, Integer> coverImageIdsByItemId = new LinkedHashMap<>();
        final Set<Integer> imageItemIds = new java.util.LinkedHashSet<>();
        for (final Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            imageItemIds.add(item.getId());
            itemService
                    .findCoverImageIdByItemId(item.getId())
                    .ifPresent(imageId -> coverImageIdsByItemId.put(item.getId(), imageId));
        }
        final Page<ItemBooking> receivedBookingPage =
                paginate(itemService.listBookingsByOwnerId(user.getId()), page, DASHBOARD_PAGE_SIZE);
        for (final ItemBooking booking : receivedBookingPage.getContent()) {
            if (booking != null && booking.getItemId() != null) {
                imageItemIds.add(booking.getItemId());
            }
        }

        mav.addObject("ownedItems", ownedItems);
        mav.addObject("publicationCoverImageIdsByItemId", coverImageIdsByItemId);
        mav.addObject("imageUrlsByItemId", buildImageUrlsByItemId(imageItemIds, contextPath));
        mav.addObject("publicationDeleteDeactivatesByItemId", buildDeleteDeactivatesByItemId(ownedItems));
        mav.addObject("publicationDeleteDisabledByItemId", buildDeleteDisabledByItemId(ownedItems));
        mav.addObject("receivedBookingRequests", receivedBookingPage.getContent());
        mav.addObject("receivedBookingPage", receivedBookingPage);
        mav.addObject("selectedBookingStatusFilters", statuses);
        mav.addObject("selectedBookingStatusFiltersByValue", buildSelectedStatusFilterMap(statuses));
        mav.addObject("boatSearchQuery", query);
        mav.addObject("pendingOwnerUserReviewsByBookingId", Map.of());
        mav.addObject("authoredUserReviewsByBookingId", Map.of());
    }

    private void addMyTripsData(
            final ModelAndView mav,
            final User user,
            final List<String> statuses,
            final String query,
            final int page,
            final String contextPath) {
        final Page<ItemBooking> sentBookingPage =
                paginate(itemService.listBookingsByGuestId(user.getId()), page, DASHBOARD_PAGE_SIZE);
        final Set<Integer> imageItemIds = new java.util.LinkedHashSet<>();
        final Map<Integer, String> sentStatusMessageCodeByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentDateLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentTimeRangeLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentTotalPriceLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentItemTitleByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentOwnerNameByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentOwnerEmailByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentPaymentAliasByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentPaymentRefusalReasonByBookingId = new LinkedHashMap<>();
        final Map<Integer, Boolean> sentHasPaymentRefusalReasonByBookingId = new LinkedHashMap<>();
        final Map<Integer, Boolean> sentPaymentProofPdfByBookingId = new LinkedHashMap<>();
        final Map<Integer, Integer> sentBookedSnapshotVersionIdByBookingId = new LinkedHashMap<>();

        for (final ItemBooking booking : sentBookingPage.getContent()) {
            if (booking != null && booking.getItemId() != null) {
                imageItemIds.add(booking.getItemId());
            }
            if (booking == null || booking.getId() == null) {
                continue;
            }

            final Integer bookingId = booking.getId();
            final Item item = booking.getItemId() == null
                    ? null
                    : itemService.findItemById(booking.getItemId()).orElse(null);
            final User owner = item != null && item.getOwnerId() != null
                    ? itemService.findUserById(item.getOwnerId()).orElse(null)
                    : null;
            final Integer pricePerHour = item == null ? null : item.getPricePerHour();
            final var paymentProof =
                    bookingRequestService.findPaymentProofByBookingId(bookingId).orElse(null);
            final String contentType = paymentProof == null ? null : paymentProof.getContentType();
            final String refusalReason = paymentProof == null ? null : paymentProof.getRefusalReason();

            sentStatusMessageCodeByBookingId.put(
                    bookingId,
                    booking.getState() == null ? "" : BookingDisplayFormatter.statusMessageCode(booking.getState()));
            sentDateLabelByBookingId.put(bookingId, BookingDisplayFormatter.formatDateLabel(booking.getStartTime()));
            sentTimeRangeLabelByBookingId.put(
                    bookingId,
                    BookingDisplayFormatter.formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()));
            sentTotalPriceLabelByBookingId.put(
                    bookingId,
                    BookingDisplayFormatter.formatTotalPriceLabel(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            sentItemTitleByBookingId.put(bookingId, item == null ? "" : item.getTitle());
            sentOwnerNameByBookingId.put(bookingId, owner == null ? "" : owner.getName());
            sentOwnerEmailByBookingId.put(bookingId, owner == null ? "" : owner.getEmail());
            sentPaymentAliasByBookingId.put(
                    bookingId, owner == null ? "" : BookingDisplayFormatter.resolvePaymentAlias(owner));
            sentPaymentRefusalReasonByBookingId.put(bookingId, refusalReason);
            sentHasPaymentRefusalReasonByBookingId.put(bookingId, refusalReason != null && !refusalReason.isBlank());
            sentPaymentProofPdfByBookingId.put(bookingId, "application/pdf".equalsIgnoreCase(contentType));
            sentBookedSnapshotVersionIdByBookingId.put(bookingId, null);
        }
        mav.addObject("sentBookingRequests", sentBookingPage.getContent());
        mav.addObject("sentBookingPage", sentBookingPage);
        mav.addObject("imageUrlsByItemId", buildImageUrlsByItemId(imageItemIds, contextPath));
        mav.addObject("sentStatusMessageCodeByBookingId", sentStatusMessageCodeByBookingId);
        mav.addObject("sentDateLabelByBookingId", sentDateLabelByBookingId);
        mav.addObject("sentTimeRangeLabelByBookingId", sentTimeRangeLabelByBookingId);
        mav.addObject("sentTotalPriceLabelByBookingId", sentTotalPriceLabelByBookingId);
        mav.addObject("sentItemTitleByBookingId", sentItemTitleByBookingId);
        mav.addObject("sentOwnerNameByBookingId", sentOwnerNameByBookingId);
        mav.addObject("sentOwnerEmailByBookingId", sentOwnerEmailByBookingId);
        mav.addObject("sentPaymentAliasByBookingId", sentPaymentAliasByBookingId);
        mav.addObject("sentPaymentRefusalReasonByBookingId", sentPaymentRefusalReasonByBookingId);
        mav.addObject("sentHasPaymentRefusalReasonByBookingId", sentHasPaymentRefusalReasonByBookingId);
        mav.addObject("sentPaymentProofPdfByBookingId", sentPaymentProofPdfByBookingId);
        mav.addObject("sentBookedSnapshotVersionIdByBookingId", sentBookedSnapshotVersionIdByBookingId);
        mav.addObject("selectedBookingStatusFilters", statuses);
        mav.addObject("selectedBookingStatusFiltersByValue", buildSelectedStatusFilterMap(statuses));
        mav.addObject("boatSearchQuery", query);
        mav.addObject("pendingGuestItemReviewsByBookingId", Map.of());
        mav.addObject("authoredItemReviewsByBookingId", Map.of());
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
        form.setPreferredLanguage(
                user.getPreferredLanguage() == null
                        ? null
                        : user.getPreferredLanguage().getPersistenceCode());
    }

    private static int sanitizePage(final int page) {
        return Math.max(1, page);
    }

    private static String normalizeQuery(final String query) {
        if (query == null) {
            return "";
        }
        return query.trim();
    }

    private static List<String> resolveBookingStatusFilters(final List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        final List<String> resolvedStatuses = new ArrayList<>();
        for (final String status : statuses) {
            if (isExactBookingStatusFilter(status) && !resolvedStatuses.contains(status)) {
                resolvedStatuses.add(status);
            }
        }
        return resolvedStatuses;
    }

    private static boolean isExactBookingStatusFilter(final String status) {
        return "pending".equals(status)
                || "confirmed".equals(status)
                || "paymentSubmitted".equals(status)
                || "paid".equals(status)
                || "paymentRefused".equals(status)
                || "completed".equals(status)
                || "rejected".equals(status)
                || "cancelled".equals(status);
    }

    private Map<Integer, Boolean> buildDeleteDeactivatesByItemId(final List<ar.edu.itba.paw.models.Item> ownedItems) {
        final Map<Integer, Boolean> map = new java.util.LinkedHashMap<>();
        if (ownedItems == null) {
            return map;
        }
        for (final ar.edu.itba.paw.models.Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            final boolean hasBlocking = itemService.listBookingsByItemId(item.getId()).stream()
                    .anyMatch(b -> b.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_REJECTED
                            && b.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_CANCELLED);
            map.put(item.getId(), Boolean.TRUE.equals(item.getActive()) && hasBlocking);
        }
        return map;
    }

    private Map<Integer, Boolean> buildDeleteDisabledByItemId(final List<ar.edu.itba.paw.models.Item> ownedItems) {
        final Map<Integer, Boolean> map = new java.util.LinkedHashMap<>();
        if (ownedItems == null) {
            return map;
        }
        final java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        for (final ar.edu.itba.paw.models.Item item : ownedItems) {
            if (item == null || item.getId() == null) {
                continue;
            }
            final boolean hasFutureBlocking = itemService.listBookingsByItemId(item.getId()).stream()
                    .anyMatch(b -> b.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_REJECTED
                            && b.getState() != ar.edu.itba.paw.models.BookingState.BOOKING_CANCELLED
                            && b.getEndTime() != null
                            && b.getEndTime().isAfter(now));
            map.put(item.getId(), !Boolean.TRUE.equals(item.getActive()) && hasFutureBlocking);
        }
        return map;
    }

    private static Map<String, Boolean> buildSelectedStatusFilterMap(final List<String> statuses) {
        final Map<String, Boolean> selectedByStatus = new LinkedHashMap<>();
        for (final String status : List.of(
                "pending",
                "confirmed",
                "paymentSubmitted",
                "paid",
                "paymentRefused",
                "completed",
                "rejected",
                "cancelled")) {
            selectedByStatus.put(status, statuses != null && statuses.contains(status));
        }
        return selectedByStatus;
    }

    private Map<Integer, String> buildImageUrlsByItemId(final Set<Integer> itemIds, final String contextPath) {
        final Map<Integer, String> urls = new LinkedHashMap<>();
        if (itemIds == null) {
            return urls;
        }
        for (final Integer itemId : itemIds) {
            if (itemId == null) {
                continue;
            }
            urls.put(itemId, ItemImageUtils.resolveImageUrl(itemService, itemId, contextPath));
        }
        return urls;
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

    private static String formatMemberSince(final OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static <T> Page<T> paginate(final List<T> items, final int page, final int pageSize) {
        final int totalItems = items == null ? 0 : items.size();
        final int totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
        final int resolvedPage = totalPages == 0 ? 1 : Math.min(Math.max(1, page), totalPages);
        final int from = totalItems == 0 ? 0 : Math.min((resolvedPage - 1) * pageSize, totalItems);
        final int to = totalItems == 0 ? 0 : Math.min(from + pageSize, totalItems);
        return new Page<>(items == null ? List.of() : items.subList(from, to), resolvedPage, pageSize, totalItems);
    }
}
