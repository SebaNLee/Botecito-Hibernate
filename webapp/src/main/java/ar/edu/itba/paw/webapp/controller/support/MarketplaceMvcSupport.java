package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.GuestMarketplaceReservationResult;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.ReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.util.AvailabilityPickerBuilder;
import ar.edu.itba.paw.webapp.form.MarketplaceSearchForm;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import ar.edu.itba.paw.webapp.util.AvailabilityPickerSupport;
import ar.edu.itba.paw.webapp.util.MarketplaceSearchCriteriaMapper;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public final class MarketplaceMvcSupport {

    private static final int DEFAULT_MARKETPLACE_PAGE_SIZE = 12;

    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;
    private final UserService userService;
    private final ReviewService reviewService;

    public static ReservationRequestForm newReservationRequestForm(final Locale locale) {
        final ReservationRequestForm form = new ReservationRequestForm();
        form.setRequesterPreferredLanguage(locale != null && "en".equalsIgnoreCase(locale.getLanguage()) ? "en" : "es");
        return form;
    }

    public ModelAndView marketplace(final HttpServletRequest request, final MarketplaceSearchForm search) {
        final ItemSearchCriteria criteria = MarketplaceSearchCriteriaMapper.fromMarketplaceForm(search);
        final int parsedPageSize = parsePageSize(search.getPageSize());
        final int page = parsePage(search.getPage());
        return buildMarketplaceModelAndView(request, criteria, page, parsedPageSize);
    }

    /**
     * When Bean Validation fails on {@link MarketplaceSearchForm}, show the marketplace with a
     * default unconstrained search while preserving the submitted form and {@link BindingResult}
     * for the view.
     */
    public ModelAndView marketplaceWithSearchBindingErrors(
            final HttpServletRequest request, final MarketplaceSearchForm search, final BindingResult errors) {
        final ItemSearchCriteria defaultCriteria = MarketplaceSearchCriteriaMapper.fromMarketplaceForm(null);
        final ModelAndView mav =
                buildMarketplaceModelAndView(request, defaultCriteria, 1, DEFAULT_MARKETPLACE_PAGE_SIZE);
        mav.addAllObjects(errors.getModel());
        return mav;
    }

    private ModelAndView buildMarketplaceModelAndView(
            final HttpServletRequest request, final ItemSearchCriteria criteria, final int page, final int pageSize) {
        final Page<Item> itemPage = itemService.searchMarketplace(criteria, page, pageSize);
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemImages", buildItemImagesMap(itemPage.getContent(), request.getContextPath()));
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("itemPage", itemPage);
        mav.addObject("pageSize", pageSize);
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

    public List<LocationOption> locationOptions() {
        return itemService.listLocationOptions();
    }

    public ModelAndView marketplaceItem(
            final HttpServletRequest request,
            final int itemId,
            final String requestedDate,
            final String requestedStartTime,
            final String requestedEndTime,
            final Integer snapshotVersionId,
            final ReservationRequestForm form) {
        if (isBlank(form.getDate())) {
            form.setDate(requestedDate);
        }
        if (isBlank(form.getStartTime())) {
            form.setStartTime(requestedStartTime);
        }
        if (isBlank(form.getEndTime())) {
            form.setEndTime(requestedEndTime);
        }

        final User currentUser = currentAuthenticatedUserOrNull();
        if (snapshotVersionId != null && currentUser == null) {
            return new ModelAndView("redirect:/login");
        }
        final Optional<ItemPageData> view = buildMarketplaceItemData(itemId, currentUser, snapshotVersionId);
        if (view.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }
        return populateMarketplaceItemView(view.get(), request.getContextPath(), form);
    }

    public ModelAndView submitMarketplaceItemRequest(
            final HttpServletRequest request,
            final int itemId,
            final ReservationRequestForm form,
            final BindingResult errors) {
        final User currentUser = currentAuthenticatedUserOrNull();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }
        if (!errors.hasFieldErrors("date")
                && !errors.hasFieldErrors("startTime")
                && !errors.hasFieldErrors("endTime")
                && !itemService.isGuestRequestedBookingRangeAvailable(
                        itemId, form.getDate(), form.getStartTime(), form.getEndTime())) {
            errors.rejectValue("startTime", "reservation.unavailable");
        }
        if (errors.hasErrors()) {
            return rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        }

        final GuestMarketplaceReservationResult result = bookingRequestService.placeGuestMarketplaceReservation(
                itemId,
                currentUser.getGivenName(),
                currentUser.getLastName(),
                currentUser.getEmail(),
                currentUser.getPreferredLanguage() == null
                        ? null
                        : currentUser.getPreferredLanguage().getPersistenceCode(),
                form.getDate(),
                form.getStartTime(),
                form.getEndTime(),
                form.getRequestMessage());

        final ModelAndView mav = rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        return switch (result.outcome()) {
            case SUCCESS -> {
                mav.addObject("mailSuccessCode", "reservation.request.success");
                mav.addObject("mailSuccessHostName", result.ownerDisplayName());
                yield mav;
            }
            case SELF_BOOKING -> {
                errors.reject("reservation.selfBooking");
                yield rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
            }
            case ERROR -> {
                mav.addObject("mailErrorCode", "reservation.request.error");
                yield mav;
            }
        };
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
        final User currentUser = currentAuthenticatedUserOrNull();
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
        mav.addObject("reviewCreatedAtLabels", buildReviewCreatedAtLabels(view.itemReviews()));

        final int resolvedItemId = view.item().getId();
        final String displayImageUrl = ItemImageUtils.resolveImageUrl(itemService, resolvedItemId, contextPath);
        final List<String> displayImageUrls = ItemImageUtils.resolveImageUrls(itemService, resolvedItemId, contextPath);
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
        Optional<Item> resolvedItem = itemService.findItemById(itemId);
        if (resolvedItem.isEmpty() && viewerUserId != null) {
            resolvedItem = itemService.findItemByIdForOwner(itemId, viewerUserId);
        }
        if (resolvedItem.isEmpty() && selectedSnapshot.isPresent()) {
            resolvedItem = itemService.findAnyItemById(itemId);
        }
        if (resolvedItem.isEmpty()) {
            return Optional.empty();
        }
        final boolean isOwner = viewerUserId != null
                && resolvedItem.get().getOwnerId() != null
                && resolvedItem.get().getOwnerId().equals(viewerUserId);
        final boolean isActive = Boolean.TRUE.equals(resolvedItem.get().getActive());
        if (!isActive && !isOwner && selectedSnapshot.isEmpty()) {
            return Optional.empty();
        }

        final User owner = resolvedItem.get().getOwnerId() == null
                ? null
                : itemService.findUserById(resolvedItem.get().getOwnerId()).orElse(null);
        final var itemType =
                itemService.findItemTypeById(resolvedItem.get().getTypeId()).orElse(null);
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
                selectedSnapshot.<Item>map(snapshot -> snapshot).orElse(resolvedItem.get());
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
                resolvedItem.get(),
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

    private static Map<Integer, String> buildReviewCreatedAtLabels(final List<Review> reviews) {
        final Locale locale = LocaleContextHolder.getLocale();
        final DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale);
        final Map<Integer, String> labels = new LinkedHashMap<>();
        for (final Review review : reviews) {
            if (review.getId() == null || review.getCreatedAt() == null) {
                continue;
            }
            labels.put(review.getId(), formatter.format(review.getCreatedAt()));
        }
        return labels;
    }

    private User currentAuthenticatedUserOrNull() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
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
}
