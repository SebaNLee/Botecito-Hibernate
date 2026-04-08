package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.webapp.form.AvailabilitySlotForm;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SessionAttributes("publishForm")
public class PublishController {

    private final ItemService itemService;
    private final MailService mailService;

    @Autowired
    public PublishController(final ItemService itemService, final MailService mailService) {
        this.itemService = itemService;
        this.mailService = mailService;
    }

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm(final Locale locale) {
        final PublishBoatForm form = new PublishBoatForm();
        form.setOwnerPreferredLanguage(toSupportedLanguage(locale));
        return form;
    }

    @ModelAttribute("itemTypeOptions")
    public Map<String, String> itemTypeOptions() {
        return buildItemTypeOptions();
    }

    @ModelAttribute("capacityOptions")
    public Map<String, String> capacityOptions() {
        return buildCapacityOptions();
    }

    @ModelAttribute("difficultyOptions")
    public Map<String, String> difficultyOptions() {
        return buildDifficultyOptions();
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publishStepOne(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return new ModelAndView("publish");
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishStepOneSubmit(
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("publish");
        }

        return new ModelAndView("redirect:/publish/availability");
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.GET)
    public ModelAndView publishStepTwo(@ModelAttribute("publishForm") final PublishBoatForm form) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        final ModelAndView mav = new ModelAndView("publish-availability");
        mav.addObject("existingSlotsJson", buildExistingSlotsJson(form));
        return mav;
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.POST)
    public ModelAndView publishStepTwoSubmit(
            @ModelAttribute("publishForm") final PublishBoatForm form, final BindingResult errors) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        validateAvailabilityStep(form, errors);
        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("publish-availability");
            mav.addObject("existingSlotsJson", buildExistingSlotsJson(form));
            return mav;
        }

        return new ModelAndView("redirect:/publish/contact");
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.GET)
    public ModelAndView publishStepThree(@ModelAttribute("publishForm") final PublishBoatForm form) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        if (buildAvailabilitySummary(form).isEmpty()) {
            return new ModelAndView("redirect:/publish/availability");
        }

        final ModelAndView mav = new ModelAndView("publish-contact");
        addSummaryData(mav, form);
        return mav;
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.POST)
    public ModelAndView publishStepThreeSubmit(
            @Validated(PublishBoatForm.Step3.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            @RequestHeader(value = "Accept-Language", required = false) final String acceptLanguage,
            final SessionStatus sessionStatus) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        validateAvailabilityStep(form, errors);

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form);
            return mav;
        }

        final Item createdItem;
        try {
            form.setOwnerPreferredLanguage(resolvePreferredLanguage(locale, acceptLanguage));
            createdItem = itemService.createPublication(
                    form.getOwnerFirstName().trim(),
                    form.getOwnerLastName().trim(),
                    form.getOwnerEmail().trim(),
                    form.getOwnerPreferredLanguage(),
                    Integer.parseInt(form.getItemTypeId().trim()),
                    form.getTitle().trim(),
                    form.getDescription() == null ? "" : form.getDescription().trim(),
                    Integer.parseInt(form.getPricePerHour().trim()),
                    Integer.parseInt(form.getCapacity().trim()),
                    parseMaxWeight(form.getMaxWeight()),
                    form.getDifficultyLevel(),
                    form.getMarina().trim(),
                    buildAvailabilitySlots(form));

            if (form.getFile() != null && !form.getFile().isEmpty()) {
                itemService.insertImage(createdItem.getId(), form.getFile().getBytes());
            }

            mailService.sendPublishConfirmationEmail(
                    form.getOwnerEmail().trim(),
                    buildOwnerName(form),
                    form.getTitle().trim(),
                    createdItem.getOwnerDeleteToken());
        } catch (final MailException | IllegalArgumentException e) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form);
            errors.reject("publish.submit.mailError");
            return mav;
        } catch (final Exception e) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form);
            errors.reject("publish.submit.persistenceError");
            return mav;
        }

        sessionStatus.setComplete();
        return new ModelAndView("redirect:/publish/success?itemId=" + createdItem.getId());
    }

    @RequestMapping(value = "/publish/success", method = RequestMethod.GET)
    public ModelAndView publishSuccess(@RequestParam(value = "itemId", required = false) final Integer itemId) {
        if (itemId == null) {
            return new ModelAndView("redirect:/publish");
        }

        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null) {
            return new ModelAndView("redirect:/publish");
        }

        final ModelAndView mav = new ModelAndView("publish-success");
        mav.addObject("item", item);

        final String typeLabel = buildItemTypeOptions().getOrDefault(String.valueOf(item.getTypeId()), "Otro");
        mav.addObject("itemTypeLabel", typeLabel);

        final List<ItemAvailability> availabilities = itemService.listAvailabilitiesByItemId(item.getId());
        mav.addObject("availabilities", availabilities);

        return mav;
    }

    private static boolean isStepOneComplete(final PublishBoatForm form) {
        return StringUtils.hasText(form.getTitle())
                && StringUtils.hasText(form.getMarina())
                && StringUtils.hasText(form.getCapacity())
                && StringUtils.hasText(form.getItemTypeId())
                && StringUtils.hasText(form.getPricePerHour());
    }

    private static String resolvePreferredLanguage(final Locale locale, final String acceptLanguageHeader) {
        final String localeLanguage = toSupportedLanguage(locale);
        if ("en".equals(localeLanguage)) {
            return "en";
        }
        return toSupportedLanguage(acceptLanguageHeader);
    }

    private static String toSupportedLanguage(final Locale locale) {
        if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
            return "en";
        }
        return "es";
    }

    private static String toSupportedLanguage(final String languageTag) {
        if (languageTag == null || languageTag.isBlank()) {
            return "es";
        }
        final String firstToken = languageTag.split(",", 2)[0].trim();
        final String tag = firstToken.split(";", 2)[0].trim();
        if ("en".equalsIgnoreCase(tag)
                || tag.regionMatches(true, 0, "en-", 0, 3)
                || tag.regionMatches(true, 0, "en_", 0, 3)) {
            return "en";
        }
        return "es";
    }

    private static String buildOwnerName(final PublishBoatForm form) {
        return form.getOwnerFirstName().trim() + " " + form.getOwnerLastName().trim();
    }

    private static BigDecimal parseMaxWeight(final String maxWeight) {
        if (!StringUtils.hasText(maxWeight)) {
            return null;
        }
        return new BigDecimal(maxWeight.trim());
    }

    private static final long MIN_RANGE_MINUTES = 120;

    private static final Map<String, String> WEEKDAY_LABELS = new LinkedHashMap<>();

    static {
        WEEKDAY_LABELS.put("MONDAY", "Lunes");
        WEEKDAY_LABELS.put("TUESDAY", "Martes");
        WEEKDAY_LABELS.put("WEDNESDAY", "Miercoles");
        WEEKDAY_LABELS.put("THURSDAY", "Jueves");
        WEEKDAY_LABELS.put("FRIDAY", "Viernes");
        WEEKDAY_LABELS.put("SATURDAY", "Sabado");
        WEEKDAY_LABELS.put("SUNDAY", "Domingo");
    }

    private static Set<String> enabledWeekdays(final PublishBoatForm form) {
        final List<String> enabled = new ArrayList<>();
        if (form.isMondayEnabled()) enabled.add("MONDAY");
        if (form.isTuesdayEnabled()) enabled.add("TUESDAY");
        if (form.isWednesdayEnabled()) enabled.add("WEDNESDAY");
        if (form.isThursdayEnabled()) enabled.add("THURSDAY");
        if (form.isFridayEnabled()) enabled.add("FRIDAY");
        if (form.isSaturdayEnabled()) enabled.add("SATURDAY");
        if (form.isSundayEnabled()) enabled.add("SUNDAY");
        return Set.copyOf(enabled);
    }

    private static List<ItemAvailability> buildAvailabilitySlots(final PublishBoatForm form) {
        final Set<String> enabled = enabledWeekdays(form);
        final List<AvailabilitySlotForm> slots = form.getAvailabilitySlots();
        final List<ItemAvailability> availabilities = new ArrayList<>();
        for (final AvailabilitySlotForm slot : slots) {
            if (slot.getWeekday() == null || !enabled.contains(slot.getWeekday())) {
                continue;
            }
            final ItemAvailability availability = new ItemAvailability();
            availability.setWeekday(slot.getWeekday());
            availability.setStartTime(slot.getStartTime());
            availability.setEndTime(slot.getEndTime());
            availabilities.add(availability);
        }
        return availabilities;
    }

    private static void addSummaryData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject(
                "itemTypeLabel", buildItemTypeOptions().getOrDefault(form.getItemTypeId(), "Sin tipo seleccionado"));
        mav.addObject("availabilitySummary", buildAvailabilitySummary(form));
    }

    private static List<String> buildAvailabilitySummary(final PublishBoatForm form) {
        final Set<String> enabled = enabledWeekdays(form);
        final List<AvailabilitySlotForm> slots = form.getAvailabilitySlots();
        final List<String> summary = new ArrayList<>();
        final Map<String, List<AvailabilitySlotForm>> byDay = slots.stream()
                .filter(s -> s.getWeekday() != null && enabled.contains(s.getWeekday()))
                .collect(Collectors.groupingBy(AvailabilitySlotForm::getWeekday));
        for (final Map.Entry<String, String> entry : WEEKDAY_LABELS.entrySet()) {
            final List<AvailabilitySlotForm> daySlots = byDay.get(entry.getKey());
            if (daySlots == null || daySlots.isEmpty()) {
                continue;
            }
            daySlots.sort(Comparator.comparing(AvailabilitySlotForm::getStartTime));
            final StringBuilder sb = new StringBuilder(entry.getValue()).append(": ");
            for (int i = 0; i < daySlots.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(daySlots.get(i).getStartTime())
                        .append(" - ")
                        .append(daySlots.get(i).getEndTime());
            }
            summary.add(sb.toString());
        }
        return summary;
    }

    private static String buildExistingSlotsJson(final PublishBoatForm form) {
        if (form.getAvailabilitySlots() == null || form.getAvailabilitySlots().isEmpty()) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (final AvailabilitySlotForm slot : form.getAvailabilitySlots()) {
            if (slot.getWeekday() == null) continue;
            if (!first) sb.append(",");
            sb.append("{\"weekday\":\"")
                    .append(slot.getWeekday())
                    .append("\",\"startTime\":\"")
                    .append(slot.getStartTime())
                    .append("\",\"endTime\":\"")
                    .append(slot.getEndTime())
                    .append("\"}");
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private static void validateAvailabilityStep(final PublishBoatForm form, final BindingResult errors) {
        final Set<String> enabled = enabledWeekdays(form);
        if (enabled.isEmpty()) {
            errors.rejectValue("mondayEnabled", "publish.availability.required");
            return;
        }

        final List<AvailabilitySlotForm> slots = form.getAvailabilitySlots();
        if (slots == null || slots.isEmpty()) {
            errors.rejectValue("mondayEnabled", "publish.availability.required");
            return;
        }

        final Map<String, List<AvailabilitySlotForm>> byDay = slots.stream()
                .filter(s -> s.getWeekday() != null && enabled.contains(s.getWeekday()))
                .collect(Collectors.groupingBy(AvailabilitySlotForm::getWeekday));

        boolean hasAnySlot = false;
        for (final String weekday : enabled) {
            final List<AvailabilitySlotForm> daySlots = byDay.get(weekday);
            if (daySlots == null || daySlots.isEmpty()) {
                errors.reject(
                        "publish.availability.day.empty",
                        new Object[] {WEEKDAY_LABELS.getOrDefault(weekday, weekday)},
                        null);
                continue;
            }
            hasAnySlot = true;
            validateDaySlots(daySlots, weekday, errors);
        }

        if (!hasAnySlot) {
            errors.rejectValue("mondayEnabled", "publish.availability.required");
        }
    }

    private static void validateDaySlots(
            final List<AvailabilitySlotForm> daySlots, final String weekday, final BindingResult errors) {
        final String dayLabel = WEEKDAY_LABELS.getOrDefault(weekday, weekday);
        final List<LocalTime[]> parsed = new ArrayList<>();

        for (final AvailabilitySlotForm slot : daySlots) {
            if (!StringUtils.hasText(slot.getStartTime()) || !StringUtils.hasText(slot.getEndTime())) {
                errors.reject("publish.availability.format.invalid", new Object[] {dayLabel}, null);
                return;
            }
            try {
                final LocalTime start = LocalTime.parse(slot.getStartTime());
                final LocalTime end = LocalTime.parse(slot.getEndTime());
                if (!end.isAfter(start)) {
                    errors.reject("publish.availability.end.invalid", new Object[] {dayLabel}, null);
                    return;
                }
                if (Duration.between(start, end).toMinutes() < MIN_RANGE_MINUTES) {
                    errors.reject("publish.availability.min.duration", new Object[] {dayLabel}, null);
                    return;
                }
                parsed.add(new LocalTime[] {start, end});
            } catch (final DateTimeParseException ex) {
                errors.reject("publish.availability.format.invalid", new Object[] {dayLabel}, null);
                return;
            }
        }

        parsed.sort(Comparator.comparing(r -> r[0]));
        for (int i = 1; i < parsed.size(); i++) {
            if (parsed.get(i)[0].isBefore(parsed.get(i - 1)[1])) {
                errors.reject("publish.availability.overlap", new Object[] {dayLabel}, null);
                return;
            }
        }
    }

    private static Map<String, String> buildItemTypeOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("1", "Otros");
        options.put("2", "Kayak");
        options.put("3", "Paddle");
        options.put("4", "Canoa");
        options.put("5", "Windsurf");
        options.put("6", "eFoil");
        options.put("7", "Optimist");
        return options;
    }

    private static Map<String, String> buildCapacityOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("1", "1 persona");
        options.put("2", "2 personas");
        options.put("4", "4 personas");
        options.put("6", "6 personas");
        options.put("8", "8 personas");
        options.put("10", "10 personas");
        options.put("12", "12 personas");
        return options;
    }

    private static Map<String, String> buildDifficultyOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("1", "1 - Principiante");
        options.put("2", "2 - Basico");
        options.put("3", "3 - Intermedio");
        options.put("4", "4 - Avanzado");
        options.put("5", "5 - Experto");
        return options;
    }
}
