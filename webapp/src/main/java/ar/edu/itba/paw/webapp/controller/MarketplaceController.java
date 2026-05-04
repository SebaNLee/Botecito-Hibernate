package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerBuilder;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerSupport;
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
    private static final int DEFAULT_MARKETPLACE_PAGE_SIZE = 12;

    private final ItemService itemService;
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
            @RequestParam(value = "searchQuery", required = false) final String searchQuery,
            @RequestParam(value = "locationOptionId", required = false) final String locationOptionId,
            @RequestParam(value = "date", required = false) final String date,
            @RequestParam(value = "startTime", required = false) final String startTime,
            @RequestParam(value = "endTime", required = false) final String endTime,
            @RequestParam(value = "capacity", required = false) final String capacity,
            @RequestParam(value = "maxWeight", required = false) final String maxWeight,
            @RequestParam(value = "difficultyLevel", required = false) final String difficultyLevel,
            @RequestParam(value = "minRating", required = false) final String minRating,
            @RequestParam(value = "sort", required = false) final String sort,
            @RequestParam(value = "page", required = false) final String page,
            @RequestParam(value = "pageSize", required = false) final String pageSize) {
        final ItemSearchCriteria criteria = itemService.parseAndValidateSearchCriteria(
                searchQuery,
                locationOptionId,
                date,
                startTime,
                endTime,
                capacity,
                maxWeight,
                difficultyLevel,
                minRating,
                sort);
        final int parsedPage = parsePage(page);
        final int parsedPageSize = parsePageSize(pageSize);
        final Page<Item> itemPage = itemService.searchMarketplace(criteria, parsedPage, parsedPageSize);
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemImages", buildItemImagesMap(itemPage.getContent(), request.getContextPath()));
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("itemPage", itemPage);
        mav.addObject("pageSize", parsedPageSize);
        mav.addObject(
                "sort",
                criteria.getSort() == null ? "newest" : criteria.getSort().getRequestValue());
        mav.addObject(
                "itemRatingSummaries",
                reviewService.getItemRatingSummaries(
                        itemPage.getContent().stream().map(Item::getId).toList()));
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerBuilder.build(itemService.listAvailabilities(), itemService.listBookings()));
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

        final User currentUser = currentAuthenticatedUser();
        if (snapshotVersionId != null && currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Optional<ItemPageData> view = buildMarketplaceItemData(itemId, currentUser, snapshotVersionId);
        if (view.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }
        return populateMarketplaceItemView(view.get(), request.getContextPath(), form);
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
            return rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        }
        if (!errors.hasFieldErrors("date")
                && !errors.hasFieldErrors("startTime")
                && !errors.hasFieldErrors("endTime")
                && !isRequestedRangeAvailable(itemId, form.getDate(), form.getStartTime(), form.getEndTime())) {
            errors.rejectValue("startTime", "reservation.unavailable");
        }
        if (errors.hasErrors()) {
            return rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        }

        final String trimmedMessage = isBlank(form.getRequestMessage())
                ? null
                : form.getRequestMessage().trim();
        bookingRequestService.createBookingRequest(
                itemId,
                currentUser.getGivenName(),
                currentUser.getLastName(),
                currentUser.getEmail(),
                currentUser.getPreferredLanguage() == null
                        ? null
                        : currentUser.getPreferredLanguage().getPersistenceCode(),
                toOffsetDateTime(form.getDate(), form.getStartTime()),
                toOffsetDateTime(form.getDate(), form.getEndTime()),
                trimmedMessage);
        final ModelAndView mav = rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        mav.addObject("mailSuccessCode", "reservation.request.success");
        mav.addObject("mailSuccessHostName", owner.map(User::getName).orElse(""));
        return mav;
    }

    private ModelAndView rebuildMarketplaceItemView(
            final int itemId, final User currentUser, final ReservationRequestForm form, final String contextPath) {
        final Optional<ItemPageData> view = buildMarketplaceItemData(itemId, currentUser, null);
        if (view.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }
        return populateMarketplaceItemView(view.get(), contextPath, form);
    }

    private ModelAndView populateMarketplaceItemView(
            final ItemPageData view, final String contextPath, final ReservationRequestForm form) {
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", view.item());
        final User currentUser = currentAuthenticatedUser();
        final boolean isOwner = currentUser != null
                && view.item().getOwnerId() != null
                && view.item().getOwnerId().equals(currentUser.getId());
        mav.addObject("isOwner", isOwner);
        mav.addObject("displayItem", view.displayItem());
        mav.addObject("selectedSnapshot", view.selectedSnapshot());
        final boolean isActive = Boolean.TRUE.equals(view.item().getActive());
        mav.addObject("hideListingLiveVersionNavigation", view.selectedSnapshot() != null && !isActive && !isOwner);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject("guestSnapshots", view.guestSnapshots());
        mav.addObject("hostSnapshots", view.hostSnapshots());
        mav.addObject("itemOwner", view.itemOwner());
        mav.addObject("itemType", view.itemType());
        mav.addObject("itemRatingSummary", view.itemRatingSummary());
        mav.addObject("itemReviews", view.itemReviews());
        mav.addObject("reviewAuthorNames", view.reviewAuthorNames());

        final int itemId = view.item().getId();
        final String displayImageUrl;
        final List<String> displayImageUrls;
        if (view.selectedSnapshot() != null && view.selectedSnapshot().getCoverImageData() != null) {
            displayImageUrl = contextPath + "/item/" + itemId + "/snapshot/"
                    + view.selectedSnapshot().getVersionId() + "/cover";
            displayImageUrls = List.of(displayImageUrl);
        } else {
            displayImageUrl = ItemImageUtils.resolveImageUrl(itemService, itemId, contextPath);
            displayImageUrls = ItemImageUtils.resolveImageUrls(itemService, itemId, contextPath);
        }
        mav.addObject("itemImageUrl", displayImageUrl);
        mav.addObject("itemImageUrls", displayImageUrls);

        final String ownerName =
                view.itemOwner() == null ? null : view.itemOwner().getName();
        mav.addObject(
                "ownerInitial",
                ownerName == null || ownerName.isEmpty()
                        ? "I"
                        : ownerName.substring(0, 1).toUpperCase());
        mav.addObject("pendingItemReviewAction", view.pendingItemReviewAction());
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "reservation", view.availability());
        final String requestedDate = form.getDate();
        final String requestedStart = form.getStartTime();
        final String requestedEnd = form.getEndTime();
        final String resolvedDate = AvailabilityPickerBuilder.resolveSelectedDate(
                requestedDate, view.availability().offeredDates(), "");
        final List<String> reservationSlots =
                view.availability().offeredTimesByDate().getOrDefault(resolvedDate, List.of());
        final boolean validRequestedRange = resolvedDate.equals(requestedDate)
                && requestedStart != null
                && !requestedStart.isBlank()
                && requestedEnd != null
                && !requestedEnd.isBlank()
                && AvailabilityPickerBuilder.hasContinuousAvailability(reservationSlots, requestedStart, requestedEnd);
        final String resolvedStart = validRequestedRange
                ? requestedStart
                : AvailabilityPickerBuilder.resolveSelectedTime(requestedStart, reservationSlots, "");
        final String resolvedEnd = validRequestedRange
                ? requestedEnd
                : AvailabilityPickerBuilder.resolveSelectedTime(requestedEnd, reservationSlots, "");
        form.setDate(resolvedDate);
        form.setStartTime(resolvedStart);
        form.setEndTime(resolvedEnd);
        mav.addObject("reservationDate", resolvedDate);
        mav.addObject("reservationStartTime", resolvedStart);
        mav.addObject("reservationEndTime", resolvedEnd);
        return mav;
    }

    private Map<Integer, String> buildItemImagesMap(final List<Item> items, final String servletContextPath) {
        final Map<Integer, String> itemImages = new LinkedHashMap<>();
        for (final Item item : items) {
            itemImages.put(item.getId(), ItemImageUtils.resolveImageUrl(itemService, item.getId(), servletContextPath));
        }
        return itemImages;
    }

    private static int parsePage(final String page) {
        if (page == null || page.isBlank()) {
            return 1;
        }
        try {
            final int parsed = Integer.parseInt(page.trim());
            return parsed < 1 ? 1 : parsed;
        } catch (final NumberFormatException ex) {
            return 1;
        }
    }

    private static int parsePageSize(final String pageSize) {
        if (pageSize == null || pageSize.isBlank()) {
            return DEFAULT_MARKETPLACE_PAGE_SIZE;
        }
        try {
            final int parsed = Integer.parseInt(pageSize.trim());
            if (parsed == 6 || parsed == 12 || parsed == 18) {
                return parsed;
            }
            return DEFAULT_MARKETPLACE_PAGE_SIZE;
        } catch (final NumberFormatException ex) {
            return DEFAULT_MARKETPLACE_PAGE_SIZE;
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
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

    private Optional<ItemPageData> buildMarketplaceItemData(
            final int itemId, final User currentUser, final Integer requestedSnapshotVersionId) {
        final Integer viewerUserId = currentUser == null ? null : currentUser.getId();
        final Optional<ItemSnapshot> selectedSnapshot = requestedSnapshotVersionId == null || viewerUserId == null
                ? Optional.empty()
                : resolveAuthorizedSnapshotVersion(requestedSnapshotVersionId, itemId, currentUser);
        if (requestedSnapshotVersionId != null && selectedSnapshot.isEmpty()) {
            return Optional.empty();
        }
        Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty() && viewerUserId != null) {
            item = itemService.findItemByIdForOwner(itemId, viewerUserId);
        }
        if (item.isEmpty() && selectedSnapshot.isPresent()) {
            item = itemService.findAnyItemById(itemId);
        }
        if (item.isEmpty()) {
            return Optional.empty();
        }
        final boolean isOwner = viewerUserId != null
                && item.get().getOwnerId() != null
                && item.get().getOwnerId().equals(viewerUserId);
        final boolean isActive = Boolean.TRUE.equals(item.get().getActive());
        if (!isActive && !isOwner && selectedSnapshot.isEmpty()) {
            return Optional.empty();
        }

        final User owner = item.get().getOwnerId() == null
                ? null
                : itemService.findUserById(item.get().getOwnerId()).orElse(null);
        final var itemType =
                itemService.findItemTypeById(item.get().getTypeId()).orElse(null);
        final var ratingSummary = itemService.getItemRatingSummary(itemId);
        final List<ar.edu.itba.paw.models.Review> reviews = itemService.listLatestReviews(itemId, 12);
        final Map<Integer, String> reviewAuthorNames = new LinkedHashMap<>();
        for (final var review : reviews) {
            if (review.getReviewerUserId() == null || reviewAuthorNames.containsKey(review.getReviewerUserId())) {
                continue;
            }
            reviewAuthorNames.put(
                    review.getReviewerUserId(),
                    itemService
                            .findUserById(review.getReviewerUserId())
                            .map(User::getName)
                            .orElse(""));
        }

        final Item displayItem =
                selectedSnapshot.<Item>map(snapshot -> snapshot).orElse(item.get());
        final boolean useSnapshotCover =
                selectedSnapshot.isPresent() && selectedSnapshot.get().getCoverImageData() != null;
        final Integer coverImageId =
                itemService.findCoverImageIdByItemId(itemId).orElse(null);
        final List<Integer> galleryImageIds =
                useSnapshotCover ? List.of() : itemService.listImageIdsByItemIdOrdered(itemId);
        final List<ItemSnapshot> guestSnapshots =
                viewerUserId == null ? List.of() : itemService.listSnapshotsByItemIdForGuest(itemId, viewerUserId);
        final List<ItemSnapshot> hostSnapshots = isOwner && viewerUserId != null
                ? itemService.listSnapshotsByItemIdForOwner(itemId, viewerUserId)
                : List.of();
        final var pendingItemReviewAction = viewerUserId == null
                ? null
                : itemService.findPendingReviewAction(viewerUserId, itemId).orElse(null);
        final AvailabilityPickerBuilder.Data availability = AvailabilityPickerBuilder.build(
                itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
        return Optional.of(new ItemPageData(
                item.get(),
                displayItem,
                selectedSnapshot.orElse(null),
                guestSnapshots,
                hostSnapshots,
                owner,
                itemType,
                ratingSummary,
                reviews,
                reviewAuthorNames,
                coverImageId,
                galleryImageIds,
                pendingItemReviewAction,
                availability));
    }

    private boolean isRequestedRangeAvailable(
            final int itemId, final String date, final String startTime, final String endTime) {
        if (date == null || date.isBlank()) {
            return true;
        }
        final AvailabilityPickerBuilder.Data data = AvailabilityPickerBuilder.build(
                itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
        final List<String> times = data.offeredTimesByDate().get(date);
        if (times == null || times.isEmpty()) {
            return false;
        }
        final boolean blankStart = startTime == null || startTime.isBlank();
        final boolean blankEnd = endTime == null || endTime.isBlank();
        if (blankStart || blankEnd) {
            return false;
        }
        return AvailabilityPickerBuilder.hasContinuousAvailability(times, startTime, endTime);
    }

    private record ItemPageData(
            Item item,
            Item displayItem,
            ItemSnapshot selectedSnapshot,
            List<ItemSnapshot> guestSnapshots,
            List<ItemSnapshot> hostSnapshots,
            User itemOwner,
            Object itemType,
            Object itemRatingSummary,
            List<ar.edu.itba.paw.models.Review> itemReviews,
            Map<Integer, String> reviewAuthorNames,
            Integer coverImageId,
            List<Integer> galleryImageIds,
            Object pendingItemReviewAction,
            AvailabilityPickerBuilder.Data availability) {}

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
