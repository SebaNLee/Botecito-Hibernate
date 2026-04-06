package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemCatalogService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MarketplaceController {

    private final ItemCatalogService itemCatalogService;

    @Autowired
    public MarketplaceController(final ItemCatalogService itemCatalogService) {
        this.itemCatalogService = itemCatalogService;
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace(
            @RequestParam(value = "location", required = false) final String requestedLocation,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime,
            @RequestParam(value = "capacity", required = false) final String requestedCapacity,
            @RequestParam(value = "maxWeight", required = false) final String requestedMaxWeight,
            @RequestParam(value = "sort", required = false, defaultValue = "recommended") final String sort) {
        final List<Item> filteredItems = itemCatalogService.listItems().stream()
                .filter(item -> matchesRequestedLocation(item, requestedLocation))
                .filter(item -> matchesMarketplaceAvailability(
                        item.getId(), requestedDate, requestedStartTime, requestedEndTime))
                .filter(item -> matchesRequestedCapacity(item, requestedCapacity))
                .filter(item -> matchesRequestedWeight(item, requestedMaxWeight))
                .toList();
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", filteredItems);
        mav.addObject("itemImages", buildItemImagesMap());
        mav.addObject("itemsCount", filteredItems.size());
        mav.addObject("sort", sort);
        AvailabilityPickerSupport.addAvailabilityPickerData(
                mav,
                "search",
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemCatalogService.listAvailabilities(), itemCatalogService.listBookings()));
        return mav;
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "date", required = false) final String requestedDate,
            @RequestParam(value = "startTime", required = false) final String requestedStartTime,
            @RequestParam(value = "endTime", required = false) final String requestedEndTime) {
        final Optional<Item> item = itemCatalogService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<User> owner = itemCatalogService.findUserById(item.get().getOwnerId());
        final Optional<ItemType> itemType =
                itemCatalogService.findItemTypeById(item.get().getTypeId());
        final AvailabilityPickerSupport.AvailabilityPickerData reservationAvailability =
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemCatalogService.listAvailabilitiesByItemId(itemId),
                        itemCatalogService.listBookingsByItemId(itemId));
        final List<String> offeredDates = reservationAvailability.getOfferedDates();
        final Map<String, List<String>> offeredTimesByDate = reservationAvailability.getOfferedTimesByDate();
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", item.get());
        mav.addObject("itemOwner", owner.orElse(null));
        mav.addObject("itemType", itemType.orElse(null));
        mav.addObject(
                "itemImageUrl", itemCatalogService.findImageUrlByItemId(itemId).orElse(""));
        mav.addObject("difficultyLabel", buildDifficultyLabel(item.get().getDifficultyLevel()));
        mav.addObject(
                "ownerInitial",
                owner.map(MarketplaceController::buildOwnerInitial).orElse("I"));
        AvailabilityPickerSupport.addAvailabilityPickerData(mav, "reservation", reservationAvailability);
        final String defaultDate = offeredDates.isEmpty() ? "" : offeredDates.getFirst();
        final String reservationDate =
                AvailabilityPickerSupport.resolveSelectedDate(requestedDate, offeredDates, defaultDate);
        final List<String> reservationSlots = offeredTimesByDate.getOrDefault(reservationDate, List.of());
        final String defaultStartTime = reservationSlots.isEmpty() ? "" : reservationSlots.getFirst();
        final String defaultEndTime = reservationSlots.size() > 1 ? reservationSlots.getLast() : defaultStartTime;
        final boolean hasValidRequestedRange = reservationDate.equals(requestedDate)
                && !isBlank(requestedStartTime)
                && !isBlank(requestedEndTime)
                && AvailabilityPickerSupport.hasContinuousAvailability(
                        reservationSlots, requestedStartTime, requestedEndTime);
        mav.addObject("reservationDate", reservationDate);
        mav.addObject(
                "reservationStartTime",
                hasValidRequestedRange
                        ? requestedStartTime
                        : AvailabilityPickerSupport.resolveSelectedTime(
                                requestedStartTime, reservationSlots, defaultStartTime));
        mav.addObject(
                "reservationEndTime",
                hasValidRequestedRange
                        ? requestedEndTime
                        : AvailabilityPickerSupport.resolveSelectedTime(
                                requestedEndTime, reservationSlots, defaultEndTime));
        return mav;
    }

    private static boolean matchesRequestedLocation(final Item item, final String requestedLocation) {
        if (isBlank(requestedLocation)) {
            return true;
        }

        return item.getLocation() != null && item.getLocation().trim().equalsIgnoreCase(requestedLocation.trim());
    }

    private static boolean matchesRequestedCapacity(final Item item, final String requestedCapacity) {
        final Integer parsedCapacity = parseInteger(requestedCapacity);
        if (parsedCapacity == null) {
            return true;
        }

        return item.getCapacityPeople() >= parsedCapacity;
    }

    private static boolean matchesRequestedWeight(final Item item, final String requestedMaxWeight) {
        final Integer parsedWeight = parseInteger(requestedMaxWeight);
        if (parsedWeight == null) {
            return true;
        }

        return item.getMaxWeightKg().compareTo(BigDecimal.valueOf(parsedWeight.longValue())) >= 0;
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

    private Map<Integer, String> buildItemImagesMap() {
        final Map<Integer, String> itemImages = new LinkedHashMap<>();
        for (final Item item : itemCatalogService.listItems()) {
            itemImages.put(
                    item.getId(),
                    itemCatalogService.findImageUrlByItemId(item.getId()).orElse(""));
        }
        return itemImages;
    }

    private static String buildDifficultyLabel(final Integer difficultyLevel) {
        if (difficultyLevel == null) {
            return "Nivel no definido";
        }
        return "Nivel " + difficultyLevel;
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
                        itemCatalogService.listAvailabilitiesByItemId(itemId),
                        itemCatalogService.listBookingsByItemId(itemId));
        final List<String> availableTimes =
                availabilityData.getOfferedTimesByDate().get(requestedDate);

        if (availableTimes == null || availableTimes.isEmpty()) {
            return false;
        }

        if (isBlank(requestedStartTime) && isBlank(requestedEndTime)) {
            return true;
        }

        if (!isBlank(requestedStartTime) && isBlank(requestedEndTime)) {
            return availableTimes.contains(requestedStartTime);
        }

        if (isBlank(requestedStartTime)) {
            return availableTimes.contains(requestedEndTime);
        }

        return AvailabilityPickerSupport.hasContinuousAvailability(
                availableTimes, requestedStartTime, requestedEndTime);
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
}
