package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.SelfBookingNotAllowedException;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class MarketplaceController {
    private static final String DEFAULT_SORT = "newest";
    private static final int MARKETPLACE_PAGE_SIZE = 10;

    private final ItemService itemService;
    private final MailService mailService;
    private final BookingRequestService bookingRequestService;
    private final UserService userService;
    private final ReviewService reviewService;

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm(final Locale locale) {
        final ReservationRequestForm form = new ReservationRequestForm();
        form.setRequesterPreferredLanguage(locale != null && "en".equalsIgnoreCase(locale.getLanguage()) ? "en" : "es");
        return form;
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace(
            final HttpServletRequest request,
            @RequestParam(value = "searchQuery", required = false) final String requestedSearchQuery,
            @RequestParam(value = "locationOptionId", required = false) final String requestedLocationOptionId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime,
            @RequestParam(value = "capacity", required = false) final String requestedCapacity,
            @RequestParam(value = "maxWeight", required = false) final String requestedMaxWeight,
            @RequestParam(value = "difficultyLevel", required = false) final String requestedDifficultyLevel,
            @RequestParam(value = "minRating", required = false) final String requestedMinRating,
            @RequestParam(value = "sort", required = false, defaultValue = DEFAULT_SORT) final String sort,
            @RequestParam(value = "page", required = false) final String requestedPage) {
        final String resolvedSort = resolveSort(sort);
        final ItemSearchCriteria criteria = buildItemSearchCriteria(
                requestedSearchQuery,
                requestedLocationOptionId,
                requestedDate,
                requestedStartTime,
                requestedEndTime,
                requestedCapacity,
                requestedMaxWeight,
                parseDifficultyLevel(requestedDifficultyLevel),
                parseMinAverageRating(requestedMinRating),
                resolvedSort);
        final Page<Item> itemPage =
                itemService.searchItems(criteria, resolvePage(requestedPage), MARKETPLACE_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemImages", buildItemImagesMap(itemPage.getContent(), request.getContextPath()));
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("itemPage", itemPage);
        mav.addObject("sort", resolvedSort);
        mav.addObject(
                "itemRatingSummaries",
                reviewService.getItemRatingSummaries(
                        itemPage.getContent().stream().map(Item::getId).toList()));
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilities(), itemService.listBookings()));
        return mav;
    }

    @ResponseBody
    @RequestMapping(
            value = "/location-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<LocationOption> locationOptions() {
        return itemService.listLocationOptions();
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime,
            @RequestParam(value = "snapshotVersionId", required = false) final Integer snapshotVersionId,
            @ModelAttribute("reservationRequestForm") final ReservationRequestForm form) {
        if (isBlank(form.getDate())) {
            form.setDate(requestedDate);
        }
        if (isBlank(form.getStartTime())) {
            form.setStartTime(requestedStartTime);
        }
        if (isBlank(form.getEndTime())) {
            form.setEndTime(requestedEndTime);
        }
        return buildMarketplaceItemView(request.getContextPath(), itemId, snapshotVersionId, form);
    }

    @RequestMapping(value = "/item/{itemId:[0-9]+}/snapshot/{versionId:[0-9]+}/cover", method = RequestMethod.GET)
    public void snapshotCoverImage(
            @PathVariable("itemId") final int itemId,
            @PathVariable("versionId") final int versionId,
            final HttpServletResponse response)
            throws java.io.IOException {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final ItemSnapshot snapshot =
                resolveAuthorizedSnapshotVersion(versionId, itemId, currentUser).orElse(null);
        if (snapshot == null || snapshot.getCoverImageData() == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType("image/jpeg");
        response.setContentLength(snapshot.getCoverImageData().length);
        response.getOutputStream().write(snapshot.getCoverImageData());
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView submitMarketplaceItemRequest(
            final HttpServletRequest request,
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("reservationRequestForm") final ReservationRequestForm form,
            final BindingResult errors) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<User> owner = itemService.findUserById(item.get().getOwnerId());
        if (item.get().getOwnerId() != null && item.get().getOwnerId().equals(currentUser.getId())) {
            errors.reject("reservation.selfBooking");
            return buildMarketplaceItemView(request.getContextPath(), itemId, null, form);
        }
        if (!errors.hasFieldErrors("date")
                && !errors.hasFieldErrors("startTime")
                && !errors.hasFieldErrors("endTime")
                && !matchesMarketplaceAvailability(itemId, form.getDate(), form.getStartTime(), form.getEndTime())) {
            errors.rejectValue("startTime", "reservation.unavailable");
        }

        if (errors.hasErrors()) {
            return buildMarketplaceItemView(request.getContextPath(), itemId, null, form);
        }

        try {
            final String trimmedMessage = isBlank(form.getRequestMessage())
                    ? null
                    : form.getRequestMessage().trim();
            final BookingRequest bookingRequest = bookingRequestService.createBookingRequest(
                    itemId,
                    currentUser.getGivenName(),
                    currentUser.getLastName(),
                    currentUser.getEmail(),
                    currentUser.getPreferredLanguage(),
                    toOffsetDateTime(form.getDate(), form.getStartTime()),
                    toOffsetDateTime(form.getDate(), form.getEndTime()),
                    trimmedMessage);
            mailService.sendBookingReviewEmail(
                    bookingRequest,
                    owner.map(User::getEmail).orElse(null),
                    item.get().getTitle(),
                    item.get().getLocation(),
                    form.getDate(),
                    form.getStartTime() + " - " + form.getEndTime());
            final ModelAndView mav = buildMarketplaceItemView(request.getContextPath(), itemId, null, form);
            mav.addObject("mailSuccessCode", "reservation.request.success");
            mav.addObject("mailSuccessHostName", owner.map(User::getName).orElse(""));
            return mav;
        } catch (final SelfBookingNotAllowedException e) {
            errors.reject("reservation.selfBooking");
            return buildMarketplaceItemView(request.getContextPath(), itemId, null, form);
        } catch (final IllegalArgumentException e) {
            final ModelAndView mav = buildMarketplaceItemView(request.getContextPath(), itemId, null, form);
            mav.addObject("mailErrorCode", "reservation.request.error");
            return mav;
        }
    }

    private ModelAndView buildMarketplaceItemView(
            final String servletContextPath,
            final int itemId,
            final Integer snapshotVersionId,
            final ReservationRequestForm form) {
        final User currentUser = currentAuthenticatedUser();

        if (snapshotVersionId != null && currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<ItemSnapshot> selectedSnapshot = snapshotVersionId == null || currentUser == null
                ? Optional.empty()
                : resolveAuthorizedSnapshotVersion(snapshotVersionId, itemId, currentUser);
        if (snapshotVersionId != null && selectedSnapshot.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty() && currentUser != null) {
            item = itemService.findItemByIdForOwner(itemId, currentUser.getId());
        }
        if (item.isEmpty() && selectedSnapshot.isPresent()) {
            item = itemService.findAnyItemById(itemId);
        }
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final boolean isOwner = currentUser != null
                && item.get().getOwnerId() != null
                && item.get().getOwnerId().equals(currentUser.getId());
        final boolean isActive = Boolean.TRUE.equals(item.get().getActive());
        if (!isActive && !isOwner && selectedSnapshot.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final boolean hideListingLiveVersionNavigation = selectedSnapshot.isPresent() && !isActive && !isOwner;

        final Optional<User> owner = itemService.findUserById(item.get().getOwnerId());
        final Optional<ItemType> itemType =
                itemService.findItemTypeById(item.get().getTypeId());
        final RatingSummary itemRatingSummary = reviewService.getItemRatingSummary(itemId);
        final List<Review> itemReviews = reviewService.listLatestItemReviews(itemId, 12);
        final Map<Integer, String> reviewAuthorNames = new LinkedHashMap<>();
        for (final Review review : itemReviews) {
            if (review.getReviewerUserId() == null || reviewAuthorNames.containsKey(review.getReviewerUserId())) {
                continue;
            }
            final String authorName = itemService
                    .findUserById(review.getReviewerUserId())
                    .map(User::getName)
                    .orElse("");
            reviewAuthorNames.put(review.getReviewerUserId(), authorName);
        }
        final Item displayItem =
                selectedSnapshot.<Item>map(snapshot -> snapshot).orElse(item.get());
        final String displayImageUrl = selectedSnapshot
                .filter(snapshot -> snapshot.getCoverImageData() != null)
                .map(snapshot ->
                        servletContextPath + "/item/" + itemId + "/snapshot/" + snapshot.getVersionId() + "/cover")
                .orElse(ItemImageUtils.resolveImageUrl(itemService, itemId, servletContextPath));
        final List<String> displayImageUrls = selectedSnapshot
                        .filter(snapshot -> snapshot.getCoverImageData() != null)
                        .isPresent()
                ? List.of(displayImageUrl)
                : ItemImageUtils.resolveImageUrls(itemService, itemId, servletContextPath);
        final AvailabilityPickerSupport.AvailabilityPickerData reservationAvailability =
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
        final List<String> offeredDates = reservationAvailability.getOfferedDates();
        final Map<String, List<String>> offeredTimesByDate = reservationAvailability.getOfferedTimesByDate();
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", item.get());
        mav.addObject("isOwner", isOwner);
        mav.addObject("displayItem", displayItem);
        mav.addObject("selectedSnapshot", selectedSnapshot.orElse(null));
        mav.addObject("hideListingLiveVersionNavigation", hideListingLiveVersionNavigation);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject(
                "guestSnapshots",
                currentUser == null
                        ? List.of()
                        : itemService.listSnapshotsByItemIdForGuest(itemId, currentUser.getId()));
        mav.addObject(
                "hostSnapshots",
                currentUser != null
                                && item.get().getOwnerId() != null
                                && item.get().getOwnerId().equals(currentUser.getId())
                        ? itemService.listSnapshotsByItemIdForOwner(itemId, currentUser.getId())
                        : List.of());
        mav.addObject("itemOwner", owner.orElse(null));
        mav.addObject("itemType", itemType.orElse(null));
        mav.addObject("itemRatingSummary", itemRatingSummary);
        mav.addObject("itemReviews", itemReviews);
        mav.addObject("reviewAuthorNames", reviewAuthorNames);
        mav.addObject("itemImageUrl", displayImageUrl);
        mav.addObject("itemImageUrls", displayImageUrls);
        mav.addObject(
                "ownerInitial",
                owner.map(MarketplaceController::buildOwnerInitial).orElse("I"));
        mav.addObject(
                "pendingItemReviewAction",
                currentUser == null
                        ? null
                        : reviewService
                                .findPendingItemReviewAction(currentUser.getId(), itemId)
                                .orElse(null));
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "reservation", reservationAvailability);
        final String defaultDate = "";
        final String reservationDate =
                AvailabilityPickerSupport.resolveSelectedDate(form.getDate(), offeredDates, defaultDate);
        final List<String> reservationSlots = offeredTimesByDate.getOrDefault(reservationDate, List.of());
        final String defaultStartTime = "";
        final String defaultEndTime = "";
        final boolean hasValidRequestedRange = reservationDate.equals(form.getDate())
                && !isBlank(form.getStartTime())
                && !isBlank(form.getEndTime())
                && AvailabilityPickerSupport.hasContinuousAvailability(
                        reservationSlots, form.getStartTime(), form.getEndTime());
        form.setDate(reservationDate);
        mav.addObject("reservationDate", reservationDate);
        mav.addObject(
                "reservationStartTime",
                hasValidRequestedRange
                        ? form.getStartTime()
                        : AvailabilityPickerSupport.resolveSelectedTime(
                                form.getStartTime(), reservationSlots, defaultStartTime));
        mav.addObject(
                "reservationEndTime",
                hasValidRequestedRange
                        ? form.getEndTime()
                        : AvailabilityPickerSupport.resolveSelectedTime(
                                form.getEndTime(), reservationSlots, defaultEndTime));
        return mav;
    }

    private static Integer parseInteger(final String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException exception) {
            return null;
        }
    }

    private static int resolvePage(final String page) {
        final Integer parsedPage = parseInteger(page);
        if (parsedPage == null || parsedPage < 1) {
            return 1;
        }
        return parsedPage;
    }

    private static ItemSearchCriteria buildItemSearchCriteria(
            final String searchQuery,
            final String requestedLocationOptionId,
            final String requestedDate,
            final String requestedStartTime,
            final String requestedEndTime,
            final String requestedCapacity,
            final String requestedMaxWeight,
            final Integer difficultyLevel,
            final Integer minAverageRating,
            final String sort) {
        final ItemSearchCriteria criteria = new ItemSearchCriteria();
        criteria.setLocationOptionId(parseInteger(requestedLocationOptionId));
        criteria.setDate(requestedDate);
        criteria.setStartTime(requestedStartTime);
        criteria.setEndTime(requestedEndTime);
        criteria.setCapacity(parseInteger(requestedCapacity));
        final Integer maxWeight = parseInteger(requestedMaxWeight);
        criteria.setMaxWeightKg(maxWeight == null ? null : BigDecimal.valueOf(maxWeight.longValue()));
        criteria.setDifficultyLevel(difficultyLevel);
        criteria.setMinAverageRating(minAverageRating);
        criteria.setSort(sort);
        criteria.setSearchQuery(searchQuery);
        return criteria;
    }

    private static Integer parseMinAverageRating(final String value) {
        final Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 1 || parsed > 5) {
            return null;
        }
        return parsed;
    }

    private static Integer parseDifficultyLevel(final String value) {
        final Integer parsed = parseInteger(value);
        if (parsed == null || parsed < 1 || parsed > 5) {
            return null;
        }
        return parsed;
    }

    private static String resolveSort(final String sort) {
        if (sort == null) {
            return DEFAULT_SORT;
        }

        return switch (sort) {
            case "newest", "oldest", "priceAsc", "priceDesc" -> sort;
            default -> DEFAULT_SORT;
        };
    }

    private Map<Integer, String> buildItemImagesMap(final List<Item> items, final String servletContextPath) {
        final Map<Integer, String> itemImages = new LinkedHashMap<>();
        for (final Item item : items) {
            itemImages.put(item.getId(), ItemImageUtils.resolveImageUrl(itemService, item.getId(), servletContextPath));
        }
        return itemImages;
    }

    private boolean matchesMarketplaceAvailability(
            final int itemId,
            final String requestedDate,
            final String requestedStartTime,
            final String requestedEndTime) {
        if (isBlank(requestedDate)) {
            return true;
        }

        final AvailabilityPickerSupport.AvailabilityPickerData availabilityData =
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
        final List<String> availableTimes =
                availabilityData.getOfferedTimesByDate().get(requestedDate);

        if (availableTimes == null || availableTimes.isEmpty()) {
            return false;
        }

        if (isBlank(requestedStartTime) && isBlank(requestedEndTime)) {
            return hasAnyContinuousTwoHourWindow(availableTimes);
        }

        if (!isBlank(requestedStartTime) && isBlank(requestedEndTime)) {
            return hasContinuousTwoHourWindowStartingAt(availableTimes, requestedStartTime);
        }

        if (isBlank(requestedStartTime)) {
            return hasContinuousTwoHourWindowEndingAt(availableTimes, requestedEndTime);
        }

        return AvailabilityPickerSupport.hasContinuousAvailability(
                availableTimes, requestedStartTime, requestedEndTime);
    }

    private static boolean hasAnyContinuousTwoHourWindow(final List<String> availableTimes) {
        for (int startIndex = 0; startIndex < availableTimes.size(); startIndex++) {
            for (int endIndex = startIndex + 1; endIndex < availableTimes.size(); endIndex++) {
                if (AvailabilityPickerSupport.hasContinuousAvailability(
                        availableTimes, availableTimes.get(startIndex), availableTimes.get(endIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowStartingAt(
            final List<String> availableTimes, final String requestedStartTime) {
        if (!availableTimes.contains(requestedStartTime)) {
            return false;
        }

        for (final String possibleEndTime : availableTimes) {
            if (AvailabilityPickerSupport.hasContinuousAvailability(
                    availableTimes, requestedStartTime, possibleEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasContinuousTwoHourWindowEndingAt(
            final List<String> availableTimes, final String requestedEndTime) {
        if (!availableTimes.contains(requestedEndTime)) {
            return false;
        }

        for (final String possibleStartTime : availableTimes) {
            if (AvailabilityPickerSupport.hasContinuousAvailability(
                    availableTimes, possibleStartTime, requestedEndTime)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private static String buildOwnerInitial(final User user) {
        if (user.getName() == null || user.getName().isEmpty()) {
            return "I";
        }
        return user.getName().substring(0, 1).toUpperCase();
    }

    private static OffsetDateTime toOffsetDateTime(final String date, final String time) {
        final LocalDate localDate = LocalDate.parse(date);
        final LocalTime localTime = LocalTime.parse(time);
        return LocalDateTime.of(localDate, localTime)
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();
    }

    private Optional<ItemSnapshot> resolveAuthorizedSnapshotVersion(
            final int versionId, final int itemId, final User currentUser) {
        final Optional<ItemSnapshot> guestSnapshot =
                itemService.findSnapshotVersionByIdForGuest(versionId, itemId, currentUser.getId());
        if (guestSnapshot.isPresent()) {
            return guestSnapshot;
        }
        return itemService.findSnapshotVersionByIdForOwner(versionId, itemId, currentUser.getId());
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
