package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.models.RequestSubmission;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.RequestService;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MarketplaceController {

    private final ItemService itemService;
    private final MailService mailService;
    private final RequestService requestService;

    @Autowired
    public MarketplaceController(
            final ItemService itemService, final MailService mailService, final RequestService requestService) {
        this.itemService = itemService;
        this.mailService = mailService;
        this.requestService = requestService;
    }

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm() {
        return new ReservationRequestForm();
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
        final List<Item> filteredItems = itemService.listItems().stream()
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
                        itemService.listAvailabilities(), itemService.listBookings()));
        return mav;
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(
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
        return buildMarketplaceItemView(itemId, form);
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.POST)
    public ModelAndView submitMarketplaceItemRequest(
            @PathVariable("id") final int itemId,
            @Valid @ModelAttribute("reservationRequestForm") final ReservationRequestForm form,
            final BindingResult errors) {
        final Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<User> owner = itemService.findUserById(item.get().getOwnerId());
        if (!errors.hasFieldErrors("date")
                && !errors.hasFieldErrors("startTime")
                && !errors.hasFieldErrors("endTime")
                && !matchesMarketplaceAvailability(itemId, form.getDate(), form.getStartTime(), form.getEndTime())) {
            errors.reject("reservation.unavailable", "The selected reservation slot is no longer available.");
        }

        if (errors.hasErrors()) {
            return buildMarketplaceItemView(itemId, form);
        }

        try {
            final RequestSubmission requestSubmission = requestService.createRequest(
                    itemId,
                    form.getRequesterName().trim(),
                    form.getRequesterEmail().trim(),
                    buildReservationRequestDescription(item.get(), owner.orElse(null), form));
            mailService.sendRequestReviewEmail(requestSubmission);
            final ModelAndView mav = buildMarketplaceItemView(itemId, form);
            mav.addObject("mailSuccess", "Your request was sent to Botecito for review.");
            return mav;
        } catch (final MailException | IllegalArgumentException e) {
            final ModelAndView mav = buildMarketplaceItemView(itemId, form);
            mav.addObject(
                    "mailError", "The request email could not be sent. Check the Gmail credentials and SMTP setup.");
            return mav;
        }
    }

    private ModelAndView buildMarketplaceItemView(final int itemId, final ReservationRequestForm form) {
        final Optional<Item> item = itemService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<User> owner = itemService.findUserById(item.get().getOwnerId());
        final Optional<ItemType> itemType =
                itemService.findItemTypeById(item.get().getTypeId());
        final AvailabilityPickerSupport.AvailabilityPickerData reservationAvailability =
                AvailabilityPickerSupport.buildAvailabilityPickerData(
                        itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
        final List<String> offeredDates = reservationAvailability.getOfferedDates();
        final Map<String, List<String>> offeredTimesByDate = reservationAvailability.getOfferedTimesByDate();
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", item.get());
        mav.addObject("itemOwner", owner.orElse(null));
        mav.addObject("itemType", itemType.orElse(null));
        mav.addObject("itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, itemId));
        mav.addObject("difficultyLabel", buildDifficultyLabel(item.get().getDifficultyLevel()));
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
        for (final Item item : itemService.listItems()) {
            itemImages.put(item.getId(), ItemImageUtils.resolveImageUrl(itemService, item.getId()));
        }
        return itemImages;
    }

    private static String buildDifficultyLabel(final Integer difficultyLevel) {
        if (difficultyLevel == null) {
            return "Nivel no definido";
        }
        return "Nivel " + difficultyLevel;
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
                        itemService.listAvailabilitiesByItemId(itemId), itemService.listBookingsByItemId(itemId));
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
