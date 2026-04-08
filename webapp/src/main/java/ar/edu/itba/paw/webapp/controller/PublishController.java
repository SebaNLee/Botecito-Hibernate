package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.utils.TimeRange;
import ar.edu.itba.paw.services.utils.TimeRangeList;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
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
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SessionAttributes("publishForm")
public class PublishController {

    private static final String PUBLISH_PREVIEW_IMAGE_PATH = "/publish/preview-image";

    private final ItemService itemService;
    private final MailService mailService;
    private final MessageSource messageSource;

    @Autowired
    public PublishController(
            final ItemService itemService, final MailService mailService, final MessageSource messageSource) {
        this.itemService = itemService;
        this.mailService = mailService;
        this.messageSource = messageSource;
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

    @ModelAttribute("uploadedImagePreviewUrl")
    public String uploadedImagePreviewUrl(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return buildUploadedImagePreviewUrl(form);
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publishStepOne(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return new ModelAndView("publish");
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishStepOneSubmit(
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        if (!errors.hasFieldErrors("file")) {
            persistUploadedImageIfPresent(form);
        }

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
        addAvailabilityEditorData(mav, form);
        return mav;
    }

    @RequestMapping(value = "/publish/availability", method = RequestMethod.POST)
    public ModelAndView publishStepTwoSubmit(
            @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            @RequestParam(value = "enabledDays", required = false) final List<String> enabledDays,
            @RequestParam(value = "availabilityRanges", required = false) final List<String> availabilityRanges) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        syncAvailabilityFromRequest(form, enabledDays, availabilityRanges, errors, locale);
        validateAvailabilityStep(form, errors, locale);
        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("publish-availability");
            addAvailabilityEditorData(mav, form);
            return mav;
        }

        return new ModelAndView("redirect:/publish/contact");
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.GET)
    public ModelAndView publishStepThree(
            @ModelAttribute("publishForm") final PublishBoatForm form, final Locale locale) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        if (buildAvailabilitySummary(form, locale).isEmpty()) {
            return new ModelAndView("redirect:/publish/availability");
        }

        final ModelAndView mav = new ModelAndView("publish-contact");
        addSummaryData(mav, form, locale);
        return mav;
    }

    @RequestMapping(value = "/publish/preview-image", method = RequestMethod.GET)
    public ResponseEntity<byte[]> publishPreviewImage(@ModelAttribute("publishForm") final PublishBoatForm form) {
        if (!hasValidImagePreview(form)) {
            return ResponseEntity.notFound().build();
        }

        final byte[] imageBytes = form.getUploadedImageData();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(resolvePreviewMediaType(form.getUploadedImageContentType()))
                .contentLength(imageBytes.length)
                .body(imageBytes);
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

        validateAvailabilityStep(form, errors, locale);

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form, locale);
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

            if (form.hasUploadedImage()) {
                itemService.insertImage(createdItem.getId(), form.getUploadedImageData());
            }

            mailService.sendPublishConfirmationEmail(
                    form.getOwnerEmail().trim(),
                    buildOwnerName(form),
                    form.getTitle().trim(),
                    createdItem.getOwnerDeleteToken());
        } catch (final IllegalArgumentException e) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            errors.reject("publish.submit.persistenceError");
            addSummaryData(mav, form, locale);
            return mav;
        } catch (final Exception e) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form, locale);
            errors.reject("publish.submit.persistenceError");
            return mav;
        }

        sessionStatus.setComplete();
        return new ModelAndView("redirect:/publish/success?token=" + createdItem.getOwnerDeleteToken());
    }

    @RequestMapping(value = "/publish/success", method = RequestMethod.GET)
    public ModelAndView publishSuccess(
            final HttpServletRequest request, @RequestParam(value = "token", required = false) final String token) {
        if (token == null || token.isBlank()) {
            return new ModelAndView("redirect:/publish");
        }

        final Item item = itemService.findItemByOwnerDeleteToken(token).orElse(null);
        if (item == null) {
            return new ModelAndView("redirect:/publish");
        }

        final ModelAndView mav = new ModelAndView("publish-success");
        mav.addObject("item", item);
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));

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
    private static final String AVAILABILITY_RANGE_SEPARATOR = "\\|";

    private static final Map<DayOfWeek, String> WEEKDAY_LABELS = new LinkedHashMap<>();

    static {
        WEEKDAY_LABELS.put(DayOfWeek.MONDAY, "Lunes");
        WEEKDAY_LABELS.put(DayOfWeek.TUESDAY, "Martes");
        WEEKDAY_LABELS.put(DayOfWeek.WEDNESDAY, "Miercoles");
        WEEKDAY_LABELS.put(DayOfWeek.THURSDAY, "Jueves");
        WEEKDAY_LABELS.put(DayOfWeek.FRIDAY, "Viernes");
        WEEKDAY_LABELS.put(DayOfWeek.SATURDAY, "Sabado");
        WEEKDAY_LABELS.put(DayOfWeek.SUNDAY, "Domingo");
    }

    private static Set<DayOfWeek> enabledWeekdays(final PublishBoatForm form) {
        return Set.copyOf(form.getAvailabilityByWeekday().keySet());
    }

    private static void addAvailabilityEditorData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject("existingSlotsJson", buildExistingSlotsJson(form));
        mav.addObject("enabledWeekdays", buildEnabledWeekdaysModel(form));
    }

    private static Map<String, Boolean> buildEnabledWeekdaysModel(final PublishBoatForm form) {
        final Set<DayOfWeek> enabled = enabledWeekdays(form);
        final Map<String, Boolean> model = new LinkedHashMap<>();
        for (final DayOfWeek weekday : WEEKDAY_LABELS.keySet()) {
            model.put(weekday.name(), enabled.contains(weekday));
        }
        return model;
    }

    private void syncAvailabilityFromRequest(
            final PublishBoatForm form,
            final List<String> enabledDays,
            final List<String> availabilityRanges,
            final BindingResult errors,
            final Locale locale) {
        form.getAvailabilityByWeekday().clear();

        if (enabledDays != null) {
            for (final String rawWeekday : enabledDays) {
                final DayOfWeek weekday = parseWeekday(rawWeekday);
                if (weekday != null) {
                    form.setDayEnabled(weekday, true);
                }
            }
        }

        if (availabilityRanges == null) {
            return;
        }

        for (final String serializedRange : availabilityRanges) {
            if (!StringUtils.hasText(serializedRange)) {
                continue;
            }

            final String[] parts = serializedRange.split(AVAILABILITY_RANGE_SEPARATOR, -1);
            if (parts.length != 3) {
                errors.reject("publish.availability.format.invalid", new Object[] {dayLabel(locale, null)}, null);
                continue;
            }

            final DayOfWeek weekday = parseWeekday(parts[0]);
            if (weekday == null || !form.isDayEnabled(weekday)) {
                continue;
            }

            try {
                final LocalTime start = LocalTime.parse(parts[1]);
                final LocalTime end = LocalTime.parse(parts[2]);

                if (!end.isAfter(start)) {
                    errors.reject("publish.availability.end.invalid", new Object[] {dayLabel(locale, weekday)}, null);
                    continue;
                }

                form.getAvailabilityFor(weekday).add(TimeRange.of(start, end));
            } catch (final DateTimeParseException ex) {
                errors.reject("publish.availability.format.invalid", new Object[] {dayLabel(locale, weekday)}, null);
            } catch (final IllegalArgumentException ex) {
                errors.reject("publish.availability.overlap", new Object[] {dayLabel(locale, weekday)}, null);
            }
        }
    }

    private static DayOfWeek parseWeekday(final String rawWeekday) {
        if (!StringUtils.hasText(rawWeekday)) {
            return null;
        }

        try {
            return DayOfWeek.valueOf(rawWeekday.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return null;
        }
    }

    private static List<ItemAvailability> buildAvailabilitySlots(final PublishBoatForm form) {
        final List<ItemAvailability> availabilities = new ArrayList<>();
        for (final DayOfWeek weekday : enabledWeekdays(form)) {
            final TimeRangeList dayRanges = form.getAvailabilityFor(weekday);
            if (dayRanges == null || dayRanges.isEmpty()) {
                continue;
            }

            for (final TimeRange range : dayRanges) {
                if (range.getStart() == null || range.getEnd() == null) {
                    continue;
                }

                final ItemAvailability availability = new ItemAvailability();
                availability.setWeekday(weekday.name());
                availability.setStartTime(formatTime(range.getStart()));
                availability.setEndTime(formatTime(range.getEnd()));
                availabilities.add(availability);
            }
        }
        return availabilities;
    }

    private void addSummaryData(final ModelAndView mav, final PublishBoatForm form, final Locale locale) {
        mav.addObject("availabilitySummary", buildAvailabilitySummary(form, locale));
        mav.addObject("uploadedImagePreviewUrl", buildUploadedImagePreviewUrl(form));
    }

    private static String buildUploadedImagePreviewUrl(final PublishBoatForm form) {
        if (!hasValidImagePreview(form)) {
            return null;
        }

        return PUBLISH_PREVIEW_IMAGE_PATH;
    }

    private static boolean hasValidImagePreview(final PublishBoatForm form) {
        if (!form.hasUploadedImage()) {
            return false;
        }

        final String contentType = form.getUploadedImageContentType();
        return StringUtils.hasText(contentType) && contentType.regionMatches(true, 0, "image/", 0, 6);
    }

    private static void persistUploadedImageIfPresent(final PublishBoatForm form) {
        final MultipartFile uploaded = form.getFile();
        if (uploaded == null || uploaded.isEmpty()) {
            return;
        }

        final String contentType = uploaded.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.regionMatches(true, 0, "image/", 0, 6)) {
            form.setFile(null);
            return;
        }

        try {
            final byte[] imageBytes = uploaded.getBytes();
            if (imageBytes.length > 0) {
                form.setUploadedImageData(imageBytes);
                form.setUploadedImageContentType(contentType);
            }
        } catch (final IOException ignored) {
            // Keep previously stored image if present.
        }

        form.setFile(null);
    }

    private static MediaType resolvePreviewMediaType(final String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (final IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private List<String> buildAvailabilitySummary(final PublishBoatForm form, final Locale locale) {
        final List<String> summary = new ArrayList<>();
        for (final Map.Entry<DayOfWeek, String> entry : WEEKDAY_LABELS.entrySet()) {
            final TimeRangeList daySlots = form.getAvailabilityFor(entry.getKey());
            if (daySlots == null || daySlots.isEmpty()) {
                continue;
            }

            final List<TimeRange> sortedSlots = new ArrayList<>(daySlots);
            sortedSlots.sort(Comparator.comparing(TimeRange::getStart));

            final StringBuilder sb = new StringBuilder(dayLabel(locale, entry.getKey())).append(": ");
            for (int i = 0; i < sortedSlots.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatTime(sortedSlots.get(i).getStart()))
                        .append(" - ")
                        .append(formatTime(sortedSlots.get(i).getEnd()));
            }
            summary.add(sb.toString());
        }
        return summary;
    }

    private static String buildExistingSlotsJson(final PublishBoatForm form) {
        if (form.getAvailabilityByWeekday().isEmpty()) {
            return "[]";
        }

        final StringBuilder sb = new StringBuilder("[");
        boolean first = true;

        for (final DayOfWeek weekday : WEEKDAY_LABELS.keySet()) {
            final TimeRangeList dayRanges = form.getAvailabilityFor(weekday);
            if (dayRanges == null || dayRanges.isEmpty()) {
                continue;
            }

            for (final TimeRange range : dayRanges) {
                if (range.getStart() == null || range.getEnd() == null) {
                    continue;
                }

                if (!first) {
                    sb.append(",");
                }

                sb.append("{\"weekday\":\"")
                        .append(weekday.name())
                        .append("\",\"startTime\":\"")
                        .append(formatTime(range.getStart()))
                        .append("\",\"endTime\":\"")
                        .append(formatTime(range.getEnd()))
                        .append("\"}");
                first = false;
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private void validateAvailabilityStep(final PublishBoatForm form, final BindingResult errors, final Locale locale) {
        final Set<DayOfWeek> enabled = enabledWeekdays(form);
        if (enabled.isEmpty()) {
            errors.rejectValue("availabilityByWeekday", "publish.availability.required");
            return;
        }

        boolean hasAnySlot = false;
        for (final DayOfWeek weekday : enabled) {
            final TimeRangeList daySlots = form.getAvailabilityFor(weekday);
            if (daySlots == null || daySlots.isEmpty()) {
                errors.reject("publish.availability.day.empty", new Object[] {dayLabel(locale, weekday)}, null);
                continue;
            }
            hasAnySlot = true;
            validateDaySlots(daySlots, weekday, errors, locale);
        }

        if (!hasAnySlot) {
            errors.rejectValue("availabilityByWeekday", "publish.availability.required");
        }
    }

    private void validateDaySlots(
            final TimeRangeList daySlots, final DayOfWeek weekday, final BindingResult errors, final Locale locale) {
        final String dayLabel = dayLabel(locale, weekday);
        final List<LocalTime[]> parsed = new ArrayList<>();

        for (final TimeRange slot : daySlots) {
            if (slot == null || slot.getStart() == null || slot.getEnd() == null) {
                errors.reject("publish.availability.format.invalid", new Object[] {dayLabel}, null);
                return;
            }

            final LocalTime start = slot.getStart();
            final LocalTime end = slot.getEnd();
            if (!end.isAfter(start)) {
                errors.reject("publish.availability.end.invalid", new Object[] {dayLabel}, null);
                return;
            }

            if (Duration.between(start, end).toMinutes() < MIN_RANGE_MINUTES) {
                errors.reject("publish.availability.min.duration", new Object[] {dayLabel}, null);
                return;
            }

            parsed.add(new LocalTime[] {start, end});
        }

        parsed.sort(Comparator.comparing(r -> r[0]));
        for (int i = 1; i < parsed.size(); i++) {
            if (parsed.get(i)[0].isBefore(parsed.get(i - 1)[1])) {
                errors.reject("publish.availability.overlap", new Object[] {dayLabel}, null);
                return;
            }
        }
    }

    private static String formatTime(final LocalTime time) {
        return time == null ? null : time.toString();
    }

    private String dayLabel(final Locale locale, final DayOfWeek weekday) {
        if (weekday == null) {
            return messageSource.getMessage("publish.step2.badge", null, locale);
        }
        return switch (weekday) {
            case MONDAY -> messageSource.getMessage("weekday.monday", null, locale);
            case TUESDAY -> messageSource.getMessage("weekday.tuesday", null, locale);
            case WEDNESDAY -> messageSource.getMessage("weekday.wednesday", null, locale);
            case THURSDAY -> messageSource.getMessage("weekday.thursday", null, locale);
            case FRIDAY -> messageSource.getMessage("weekday.friday", null, locale);
            case SATURDAY -> messageSource.getMessage("weekday.saturday", null, locale);
            case SUNDAY -> messageSource.getMessage("weekday.sunday", null, locale);
        };
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
