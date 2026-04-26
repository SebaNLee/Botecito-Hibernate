package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemSearchCriteria;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.LocationOption;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.DisabledTimeSlotService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.Page;
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
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MarketplaceController {
    private static final String DEFAULT_SORT = "newest";
    private static final int MARKETPLACE_PAGE_SIZE = 10;

    private final ItemService itemService;
    private final MailService mailService;
    private final BookingRequestService bookingRequestService;
    private final UserService userService;
    private final DisabledTimeSlotService disabledTimeSlotService;

    @Autowired
    public MarketplaceController(
            final ItemService itemService,
            final MailService mailService,
            final BookingRequestService bookingRequestService,
            final UserService userService,
            final DisabledTimeSlotService disabledTimeSlotService) {
        this.itemService = itemService;
        this.mailService = mailService;
        this.bookingRequestService = bookingRequestService;
        this.userService = userService;
        this.disabledTimeSlotService = disabledTimeSlotService;
    }

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
                resolvedSort);
        final Page<Item> itemPage =
                itemService.searchItems(criteria, resolvePage(requestedPage), MARKETPLACE_PAGE_SIZE);
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", itemPage.getContent());
        mav.addObject("itemImages", buildItemImagesMap(itemPage.getContent(), request.getContextPath()));
        mav.addObject("itemsCount", itemPage.getTotalItems());
        mav.addObject("itemPage", itemPage);
        mav.addObject("sort", resolvedSort);
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilities(),
                        itemService.listBookings(),
                        disabledTimeSlotService.listAll()));
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
        return buildMarketplaceItemView(request.getContextPath(), itemId, form);
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
        if (!errors.hasFieldErrors("date")
                && !errors.hasFieldErrors("startTime")
                && !errors.hasFieldErrors("endTime")
                && !matchesMarketplaceAvailability(itemId, form.getDate(), form.getStartTime(), form.getEndTime())) {
            errors.rejectValue("startTime", "reservation.unavailable");
        }

        if (errors.hasErrors()) {
            return buildMarketplaceItemView(request.getContextPath(), itemId, form);
        }

        try {
            final BookingRequest bookingRequest = bookingRequestService.createBookingRequest(
                    itemId,
                    currentUser.getGivenName(),
                    currentUser.getLastName(),
                    currentUser.getEmail(),
                    currentUser.getPreferredLanguage(),
                    toOffsetDateTime(form.getDate(), form.getStartTime()),
                    toOffsetDateTime(form.getDate(), form.getEndTime()),
                    buildReservationRequestDescription(item.get(), owner.orElse(null), form));
            mailService.sendBookingReviewEmail(
                    bookingRequest, owner.map(User::getEmail).orElse(null));
            final ModelAndView mav = buildMarketplaceItemView(request.getContextPath(), itemId, form);
            mav.addObject("mailSuccessCode", "reservation.request.success");
            return mav;
        } catch (final IllegalArgumentException e) {
            final ModelAndView mav = buildMarketplaceItemView(request.getContextPath(), itemId, form);
            mav.addObject("mailErrorCode", "reservation.request.error");
            return mav;
        }
    }

    private ModelAndView buildMarketplaceItemView(
            final String servletContextPath, final int itemId, final ReservationRequestForm form) {
        final Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<User> owner = itemService.findUserById(item.get().getOwnerId());
        final Optional<ItemType> itemType =
                itemService.findItemTypeById(item.get().getTypeId());
        final AvailabilityPickerSupport.AvailabilityPickerData reservationAvailability =
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilitiesByItemId(itemId),
                        itemService.listBookingsByItemId(itemId),
                        disabledTimeSlotService.listByItem(itemId));
        final List<String> offeredDates = reservationAvailability.getOfferedDates();
        final Map<String, List<String>> offeredTimesByDate = reservationAvailability.getOfferedTimesByDate();
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", item.get());
        mav.addObject("itemOwner", owner.orElse(null));
        mav.addObject("itemType", itemType.orElse(null));
        mav.addObject("itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, itemId, servletContextPath));
        mav.addObject(
                "ownerInitial",
                owner.map(MarketplaceController::buildOwnerInitial).orElse("I"));
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "reservation", reservationAvailability);
        final String defaultDate = offeredDates.isEmpty() ? "" : offeredDates.getFirst();
        final String reservationDate =
                AvailabilityPickerSupport.resolveSelectedDate(form.getDate(), offeredDates, defaultDate);
        final List<String> reservationSlots = offeredTimesByDate.getOrDefault(reservationDate, List.of());
        final String defaultStartTime = reservationSlots.isEmpty() ? "" : reservationSlots.getFirst();
        final String defaultEndTime = reservationSlots.size() > 1 ? reservationSlots.getLast() : defaultStartTime;
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
        criteria.setSort(sort);
        criteria.setSearchQuery(searchQuery);
        return criteria;
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

    private static String buildReservationRequestDescription(
            final Item item, final User owner, final ReservationRequestForm form) {
        final StringBuilder description = new StringBuilder();
        description
                .append("Item: ")
                .append(item.getTitle())
                .append(" (#")
                .append(item.getId())
                .append(")\n");
        description.append("Location: ").append(item.getLocation()).append('\n');
        if (owner != null) {
            description
                    .append("Owner: ")
                    .append(owner.getName())
                    .append(" <")
                    .append(owner.getEmail())
                    .append(">\n");
        }
        description.append("Requested date: ").append(form.getDate()).append('\n');
        description
                .append("Requested time: ")
                .append(form.getStartTime())
                .append(" - ")
                .append(form.getEndTime());

        if (!isBlank(form.getRequestMessage())) {
            description.append("\n\nMessage:\n").append(form.getRequestMessage().trim());
        }

        return description.toString();
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
                        itemService.listAvailabilitiesByItemId(itemId),
                        itemService.listBookingsByItemId(itemId),
                        disabledTimeSlotService.listByItem(itemId));
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
