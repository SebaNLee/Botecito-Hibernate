package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.CatalogUser;
import ar.edu.itba.paw.models.ClassUser;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemType;
import ar.edu.itba.paw.services.ClassUserService;
import ar.edu.itba.paw.services.ItemCatalogService;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HelloWorldController {
    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter RESERVATION_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final ItemCatalogService itemCatalogService;
    private final ClassUserService classUserService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace(
            @RequestParam(value = "sort", required = false, defaultValue = "recommended") final String sort) {
        final ModelAndView mav = new ModelAndView("marketplace");
        mav.addObject("items", itemCatalogService.listItems());
        mav.addObject("itemImages", buildItemImagesMap());
        mav.addObject("itemsCount", itemCatalogService.listItems().size());
        mav.addObject("sort", sort);
        return mav;
    }

    @RequestMapping(value = "/item/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(@PathVariable("id") final int itemId) {
        final Optional<Item> item = itemCatalogService.findItemById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/marketplace");
        }

        final Optional<CatalogUser> owner =
                itemCatalogService.findUserById(item.get().getOwnerId());
        final Optional<ItemType> itemType =
                itemCatalogService.findItemTypeById(item.get().getTypeId());
        final Optional<ItemAvailability> nextAvailability = itemCatalogService.findNextAvailabilityByItemId(itemId);
        final ModelAndView mav = new ModelAndView("marketplace-item");
        mav.addObject("item", item.get());
        mav.addObject("itemOwner", owner.orElse(null));
        mav.addObject("itemType", itemType.orElse(null));
        mav.addObject(
                "itemImageUrl", itemCatalogService.findImageUrlByItemId(itemId).orElse(""));
        mav.addObject("difficultyLabel", buildDifficultyLabel(item.get().getDifficultyLevel()));
        mav.addObject(
                "ownerInitial",
                owner.map(HelloWorldController::buildOwnerInitial).orElse("I"));
        mav.addObject(
                "reservationDate",
                nextAvailability
                        .map(HelloWorldController::formatReservationDate)
                        .orElse(""));
        mav.addObject(
                "reservationWindow",
                nextAvailability
                        .map(HelloWorldController::formatReservationWindow)
                        .orElse("Sin horarios"));
        mav.addObject(
                "reservationLabel",
                nextAvailability
                        .map(HelloWorldController::buildReservationLabel)
                        .orElse("Sin disponibilidad"));
        return mav;
    }

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm() {
        return new PublishBoatForm();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publish(
            @RequestParam(value = "submitted", required = false, defaultValue = "false") final boolean submitted,
            @ModelAttribute("publishForm") final PublishBoatForm form) {
        final ModelAndView mav = new ModelAndView("publish");
        mav.addObject("submitted", submitted);
        mav.addObject("capacityOptions", buildCapacityOptions());
        mav.addObject("weightOptions", buildWeightOptions());
        return mav;
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishSubmit(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return new ModelAndView("redirect:/publish?submitted=true");
    }

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================

    // Note: change class root directory from / to /class/
    // Example: /example from class would be /class/example

    @Autowired
    public HelloWorldController(final ClassUserService classUserService, final ItemCatalogService itemCatalogService) {
        this.classUserService = classUserService;
        this.itemCatalogService = itemCatalogService;
    }

    @RequestMapping(value = "/class", method = RequestMethod.GET)
    public ModelAndView helloWorld() {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        mav.addObject("message", "Hello World from Controller");
        return mav;
    }

    @RequestMapping(value = "/class", method = RequestMethod.POST)
    public ModelAndView createClassUser(
            @RequestParam("email") final String email,
            @RequestParam("password") final String password,
            @RequestParam("username") final String username) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final ClassUser classUser = classUserService.createClassUser(email, password, username);
        mav.addObject("message", "Hello World " + classUser.getUsername());
        return mav;
    }

    @RequestMapping(value = "/class/profile/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView helloWorld(@PathVariable("id") final long id) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final Optional<ClassUser> classUser = classUserService.findClassUserById(id);
        mav.addObject("message", "This it the profile for " + classUser.get().getUsername());
        return mav;
    }

    // ====================================
    // TODO reference, demo code from class
    // end
    // ====================================
    private static Map<String, String> buildCapacityOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("2", "2 personas");
        options.put("4", "4 personas");
        options.put("6", "6 personas");
        options.put("8", "8 personas");
        options.put("10", "10 personas");
        options.put("12", "12 personas");
        return options;
    }

    private static Map<String, String> buildWeightOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("400", "400 kg");
        options.put("600", "600 kg");
        options.put("800", "800 kg");
        options.put("1000", "1,000 kg");
        options.put("1200", "1,200 kg");
        return options;
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

    private static String formatReservationDate(final ItemAvailability itemAvailability) {
        return OffsetDateTime.parse(itemAvailability.getStartTime()).format(INPUT_DATE_FORMAT);
    }

    private static String formatReservationWindow(final ItemAvailability itemAvailability) {
        final OffsetDateTime startTime = OffsetDateTime.parse(itemAvailability.getStartTime());
        final OffsetDateTime endTime = OffsetDateTime.parse(itemAvailability.getEndTime());
        return startTime.format(RESERVATION_TIME_FORMAT) + " a " + endTime.format(RESERVATION_TIME_FORMAT);
    }

    private static String buildReservationLabel(final ItemAvailability itemAvailability) {
        final OffsetDateTime startTime = OffsetDateTime.parse(itemAvailability.getStartTime());
        final OffsetDateTime endTime = OffsetDateTime.parse(itemAvailability.getEndTime());
        final long hours = Math.max(1, Duration.between(startTime, endTime).toHours());
        return "Reserva de " + hours + (hours == 1 ? " hora" : " horas");
    }

    private static String buildOwnerInitial(final CatalogUser catalogUser) {
        if (catalogUser.getName() == null || catalogUser.getName().isEmpty()) {
            return "I";
        }
        return catalogUser.getName().substring(0, 1).toUpperCase();
    }
}
