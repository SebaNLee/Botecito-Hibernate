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
import ar.edu.itba.paw.services.SelfBookingNotAllowedException;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.dto.MarketplaceItemView;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
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
    private static final int MARKETPLACE_PAGE_SIZE = 10;

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
            @RequestParam(value = "page", required = false) final String page) {
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
        final Page<Item> itemPage = itemService.searchMarketplace(criteria, parsePage(page), MARKETPLACE_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemImages", buildItemImagesMap(itemPage.getContent(), request.getContextPath()));
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("itemPage", itemPage);
        mav.addObject(
                "sort",
                criteria.getSort() == null ? "newest" : criteria.getSort().getRequestValue());
        mav.addObject(
                "itemRatingSummaries",
                reviewService.getItemRatingSummaries(
                        itemPage.getContent().stream().map(Item::getId).toList()));
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "search", itemService.buildGlobalAvailabilityPicker());
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
        final Optional<MarketplaceItemView> view = itemService.findMarketplaceItemView(
                itemId,
                currentUser == null ? null : currentUser.getId(),
                snapshotVersionId,
                form.getDate(),
                form.getStartTime(),
                form.getEndTime());
        if (view.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }
        form.setDate(view.get().getResolvedDate());
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
                && !itemService.isRequestedRangeAvailable(
                        itemId, form.getDate(), form.getStartTime(), form.getEndTime())) {
            errors.rejectValue("startTime", "reservation.unavailable");
        }
        if (errors.hasErrors()) {
            return rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        }

        try {
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
        } catch (final SelfBookingNotAllowedException e) {
            errors.reject("reservation.selfBooking");
            return rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
        } catch (final Exception e) {
            final ModelAndView mav = rebuildMarketplaceItemView(itemId, currentUser, form, request.getContextPath());
            mav.addObject("mailErrorCode", "reservation.request.error");
            return mav;
        }
    }

    private ModelAndView rebuildMarketplaceItemView(
            final int itemId, final User currentUser, final ReservationRequestForm form, final String contextPath) {
        final Optional<MarketplaceItemView> view = itemService.findMarketplaceItemView(
                itemId,
                currentUser == null ? null : currentUser.getId(),
                null,
                form.getDate(),
                form.getStartTime(),
                form.getEndTime());
        if (view.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }
        return populateMarketplaceItemView(view.get(), contextPath, form);
    }

    private ModelAndView populateMarketplaceItemView(
            final MarketplaceItemView view, final String contextPath, final ReservationRequestForm form) {
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", view.getItem());
        mav.addObject("isOwner", view.isOwner());
        mav.addObject("displayItem", view.getDisplayItem());
        mav.addObject("selectedSnapshot", view.getSelectedSnapshot());
        mav.addObject("hideListingLiveVersionNavigation", view.isHideListingLiveVersionNavigation());
        mav.addObject("listingInactiveNotice", view.isListingInactiveNotice());
        mav.addObject("guestSnapshots", view.getGuestSnapshots());
        mav.addObject("hostSnapshots", view.getHostSnapshots());
        mav.addObject("itemOwner", view.getItemOwner());
        mav.addObject("itemType", view.getItemType());
        mav.addObject("itemRatingSummary", view.getItemRatingSummary());
        mav.addObject("itemReviews", view.getItemReviews());
        mav.addObject("reviewAuthorNames", view.getReviewAuthorNames());

        final int itemId = view.getItem().getId();
        final String displayImageUrl;
        final List<String> displayImageUrls;
        if (view.isUseSnapshotCoverImage()) {
            displayImageUrl = contextPath + "/item/" + itemId + "/snapshot/"
                    + view.getSelectedSnapshot().getVersionId() + "/cover";
            displayImageUrls = List.of(displayImageUrl);
        } else {
            displayImageUrl = ItemImageUtils.resolveImageUrl(itemService, itemId, contextPath);
            displayImageUrls = ItemImageUtils.resolveImageUrls(itemService, itemId, contextPath);
        }
        mav.addObject("itemImageUrl", displayImageUrl);
        mav.addObject("itemImageUrls", displayImageUrls);

        mav.addObject("ownerInitial", view.getOwnerInitial());
        mav.addObject("pendingItemReviewAction", view.getPendingItemReviewAction());
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "reservation", view.getAvailability());
        mav.addObject("reservationDate", view.getResolvedDate());
        mav.addObject("reservationStartTime", view.getResolvedStartTime());
        mav.addObject("reservationEndTime", view.getResolvedEndTime());
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
