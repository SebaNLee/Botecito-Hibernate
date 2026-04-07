package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
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
    public ModelAndView publishStepOne(
            @RequestParam(value = "submitted", required = false, defaultValue = "false") final boolean submitted,
            @ModelAttribute("publishForm") final PublishBoatForm form) {
        final ModelAndView mav = new ModelAndView("publish");
        mav.addObject("submitted", submitted);
        return mav;
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
        return new ModelAndView("publish-availability");
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.POST)
    public ModelAndView publishStepTwoSubmit(
            @ModelAttribute("publishForm") final PublishBoatForm form, final BindingResult errors) {
        validateAvailabilityStep(form, errors);
        if (errors.hasErrors()) {
            return new ModelAndView("publish-availability");
        }

        return new ModelAndView("redirect:/publish/contact");
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.GET)
    public ModelAndView publishStepThree(@ModelAttribute("publishForm") final PublishBoatForm form) {
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
            final SessionStatus sessionStatus) {
        validateAvailabilityStep(form, errors);

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form);
            return mav;
        }

        try {
            form.setOwnerPreferredLanguage(resolveOwnerPreferredLanguage(form));
            final Item createdItem = itemService.createPublication(
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
        return new ModelAndView("redirect:/publish?submitted=true");
    }

    private String resolveOwnerPreferredLanguage(final PublishBoatForm form) {
        return itemService
                .findUserByEmail(form.getOwnerEmail().trim())
                .map(user -> toSupportedLanguage(user.getPreferredLanguage()))
                .orElseGet(() -> toSupportedLanguage(form.getOwnerPreferredLanguage()));
    }

    private static String toSupportedLanguage(final Locale locale) {
        if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
            return "en";
        }
        return "es";
    }

    private static String toSupportedLanguage(final String languageTag) {
        if ("en".equalsIgnoreCase(languageTag)) {
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

    private static List<ItemAvailability> buildAvailabilitySlots(final PublishBoatForm form) {
        final List<ItemAvailability> availabilities = new ArrayList<>();
        addAvailability(
                availabilities, "MONDAY", form.isMondayEnabled(), form.getMondayStartTime(), form.getMondayEndTime());
        addAvailability(
                availabilities,
                "TUESDAY",
                form.isTuesdayEnabled(),
                form.getTuesdayStartTime(),
                form.getTuesdayEndTime());
        addAvailability(
                availabilities,
                "WEDNESDAY",
                form.isWednesdayEnabled(),
                form.getWednesdayStartTime(),
                form.getWednesdayEndTime());
        addAvailability(
                availabilities,
                "THURSDAY",
                form.isThursdayEnabled(),
                form.getThursdayStartTime(),
                form.getThursdayEndTime());
        addAvailability(
                availabilities, "FRIDAY", form.isFridayEnabled(), form.getFridayStartTime(), form.getFridayEndTime());
        addAvailability(
                availabilities,
                "SATURDAY",
                form.isSaturdayEnabled(),
                form.getSaturdayStartTime(),
                form.getSaturdayEndTime());
        addAvailability(
                availabilities, "SUNDAY", form.isSundayEnabled(), form.getSundayStartTime(), form.getSundayEndTime());
        return availabilities;
    }

    private static void addAvailability(
            final List<ItemAvailability> availabilities,
            final String weekday,
            final boolean enabled,
            final String startTime,
            final String endTime) {
        if (!enabled) {
            return;
        }

        final ItemAvailability availability = new ItemAvailability();
        availability.setWeekday(weekday);
        availability.setStartTime(startTime);
        availability.setEndTime(endTime);
        availabilities.add(availability);
    }

    private static void addSummaryData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject(
                "itemTypeLabel", buildItemTypeOptions().getOrDefault(form.getItemTypeId(), "Sin tipo seleccionado"));
        mav.addObject("availabilitySummary", buildAvailabilitySummary(form));
    }

    private static List<String> buildAvailabilitySummary(final PublishBoatForm form) {
        final List<String> summary = new ArrayList<>();
        addDaySummary(summary, "Lunes", form.isMondayEnabled(), form.getMondayStartTime(), form.getMondayEndTime());
        addDaySummary(summary, "Martes", form.isTuesdayEnabled(), form.getTuesdayStartTime(), form.getTuesdayEndTime());
        addDaySummary(
                summary,
                "Miercoles",
                form.isWednesdayEnabled(),
                form.getWednesdayStartTime(),
                form.getWednesdayEndTime());
        addDaySummary(
                summary, "Jueves", form.isThursdayEnabled(), form.getThursdayStartTime(), form.getThursdayEndTime());
        addDaySummary(summary, "Viernes", form.isFridayEnabled(), form.getFridayStartTime(), form.getFridayEndTime());
        addDaySummary(
                summary, "Sabado", form.isSaturdayEnabled(), form.getSaturdayStartTime(), form.getSaturdayEndTime());
        addDaySummary(summary, "Domingo", form.isSundayEnabled(), form.getSundayStartTime(), form.getSundayEndTime());
        return summary;
    }

    private static void addDaySummary(
            final List<String> summary,
            final String label,
            final boolean enabled,
            final String startTime,
            final String endTime) {
        if (!enabled) {
            return;
        }

        summary.add(label + ": " + startTime + " - " + endTime);
    }

    private static void validateAvailabilityStep(final PublishBoatForm form, final BindingResult errors) {
        boolean hasEnabledDay = false;

        hasEnabledDay |= validateDay(
                form.isMondayEnabled(), form.getMondayStartTime(), form.getMondayEndTime(), "monday", errors);
        hasEnabledDay |= validateDay(
                form.isTuesdayEnabled(), form.getTuesdayStartTime(), form.getTuesdayEndTime(), "tuesday", errors);
        hasEnabledDay |= validateDay(
                form.isWednesdayEnabled(),
                form.getWednesdayStartTime(),
                form.getWednesdayEndTime(),
                "wednesday",
                errors);
        hasEnabledDay |= validateDay(
                form.isThursdayEnabled(), form.getThursdayStartTime(), form.getThursdayEndTime(), "thursday", errors);
        hasEnabledDay |= validateDay(
                form.isFridayEnabled(), form.getFridayStartTime(), form.getFridayEndTime(), "friday", errors);
        hasEnabledDay |= validateDay(
                form.isSaturdayEnabled(), form.getSaturdayStartTime(), form.getSaturdayEndTime(), "saturday", errors);
        hasEnabledDay |= validateDay(
                form.isSundayEnabled(), form.getSundayStartTime(), form.getSundayEndTime(), "sunday", errors);

        if (!hasEnabledDay) {
            errors.rejectValue("mondayEnabled", "publish.availability.required");
        }
    }

    private static boolean validateDay(
            final boolean enabled,
            final String startTime,
            final String endTime,
            final String fieldPrefix,
            final BindingResult errors) {
        if (!enabled) {
            return false;
        }

        final String startField = fieldPrefix + "StartTime";
        final String endField = fieldPrefix + "EndTime";

        if (!StringUtils.hasText(startTime)) {
            errors.rejectValue(startField, "publish.availability.start.required");
        }

        if (!StringUtils.hasText(endTime)) {
            errors.rejectValue(endField, "publish.availability.end.required");
        }

        if (!StringUtils.hasText(startTime) || !StringUtils.hasText(endTime)) {
            return true;
        }

        try {
            final LocalTime start = LocalTime.parse(startTime);
            final LocalTime end = LocalTime.parse(endTime);
            if (!end.isAfter(start)) {
                errors.rejectValue(endField, "publish.availability.end.invalid");
            }
        } catch (final DateTimeParseException ex) {
            errors.rejectValue(endField, "publish.availability.format.invalid");
        }

        return true;
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
