package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.GalleryImageUpload;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.PublicationDraft;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.utils.TimeRange;
import ar.edu.itba.paw.services.utils.TimeRangeList;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.form.PublishBoatForm.UploadedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
@SessionAttributes("publishForm")
@RequiredArgsConstructor
public class PublishController {

    private static final String PUBLISH_PREVIEW_IMAGE_PATH = "/publish/preview-image";
    private static final int MAX_GALLERY_IMAGES = 10;
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

    private final ItemService itemService;
    private final UserService userService;
    private final MessageSource messageSource;

    @ModelAttribute("publishForm")
    public PublishBoatForm publishForm(final Locale locale) {
        return new PublishBoatForm();
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

    @ModelAttribute("uploadedImagePreviewUrls")
    public List<String> uploadedImagePreviewUrls(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return buildUploadedImagePreviewUrls(form);
    }

    @ModelAttribute("maxGalleryImages")
    public int maxGalleryImages() {
        return MAX_GALLERY_IMAGES;
    }

    @RequestMapping(value = "/publish", method = RequestMethod.GET)
    public ModelAndView publishStepOne(@ModelAttribute("publishForm") final PublishBoatForm form) {
        return new ModelAndView("publish");
    }

    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publishStepOneSubmit(
            @Validated(PublishBoatForm.Step1.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors) {
        if (!errors.hasFieldErrors("files")) {
            appendUploadedImagesIfPresent(form, errors);
        }
        form.setFiles(new ArrayList<>());

        if (errors.hasErrors()) {
            return new ModelAndView("publish");
        }

        return new ModelAndView("redirect:/publish/availability");
    }

    @RequestMapping(value = "/publish/images/upload", method = RequestMethod.POST)
    public ModelAndView publishImagesUpload(
            @ModelAttribute("publishForm") final PublishBoatForm form, final BindingResult errors) {
        appendUploadedImagesIfPresent(form, errors);
        form.setFiles(new ArrayList<>());
        return new ModelAndView("redirect:/publish");
    }

    @RequestMapping(value = "/publish/images/remove", method = RequestMethod.POST)
    public ModelAndView publishImagesRemove(
            @ModelAttribute("publishForm") final PublishBoatForm form, @RequestParam("index") final int index) {
        form.removeUploadedImageAt(index);
        return new ModelAndView("redirect:/publish");
    }

    @RequestMapping(value = "/publish/images/reorder", method = RequestMethod.POST)
    public ModelAndView publishImagesReorder(
            @ModelAttribute("publishForm") final PublishBoatForm form,
            @RequestParam(value = "order", required = false) final String order) {
        final List<Integer> parsed = parseOrderCsv(order);
        form.reorderUploadedImages(parsed);
        return new ModelAndView("redirect:/publish");
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

        syncAvailabilityFromRequest(form, enabledDays, availabilityRanges);
        applyDraftValidation(form, errors, locale);
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

    @RequestMapping(value = "/publish/preview-image/{index}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> publishPreviewImage(
            @ModelAttribute("publishForm") final PublishBoatForm form, @PathVariable("index") final int index) {
        final UploadedImage image = form.getUploadedImageAt(index);
        if (image == null || image.getSize() == 0) {
            return ResponseEntity.notFound().build();
        }
        final byte[] imageBytes = image.getData();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(resolvePreviewMediaType(image.getContentType()))
                .contentLength(imageBytes.length)
                .body(imageBytes);
    }

    @RequestMapping(value = "/publish/contact", method = RequestMethod.POST)
    public ModelAndView publishStepThreeSubmit(
            @Validated(PublishBoatForm.Step3.class) @ModelAttribute("publishForm") final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            final SessionStatus sessionStatus) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        applyDraftValidation(form, errors, locale);

        if (errors.hasErrors()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            addSummaryData(mav, form, locale);
            return mav;
        }

        final Item createdItem;
        try {
            createdItem = itemService.createPublicationFromDraft(toDraft(form, currentUser));
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
        return new ModelAndView("redirect:/publish/success?itemId=" + createdItem.getId());
    }

    @RequestMapping(value = "/publish/success", method = RequestMethod.GET)
    public ModelAndView publishSuccess(
            final HttpServletRequest request, @RequestParam(value = "itemId", required = false) final Integer itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null || itemId == null) {
            return new ModelAndView("redirect:/login");
        }

        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null) {
            return new ModelAndView("redirect:/publish");
        }
        if (!item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/403");
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
                && StringUtils.hasText(form.getLocationOptionId())
                && StringUtils.hasText(form.getCapacity())
                && StringUtils.hasText(form.getItemTypeId())
                && StringUtils.hasText(form.getPricePerHour());
    }

    private static BigDecimal parseMaxWeight(final String maxWeight) {
        if (!StringUtils.hasText(maxWeight)) {
            return null;
        }
        return new BigDecimal(maxWeight.trim());
    }

    private static void addAvailabilityEditorData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject("existingSlotsJson", buildExistingSlotsJson(form));
        mav.addObject("enabledWeekdays", buildEnabledWeekdaysModel(form));
    }

    private static Map<String, Boolean> buildEnabledWeekdaysModel(final PublishBoatForm form) {
        final Map<String, Boolean> model = new LinkedHashMap<>();
        for (final DayOfWeek weekday : WEEKDAY_LABELS.keySet()) {
            model.put(weekday.name(), form.isDayEnabled(weekday));
        }
        return model;
    }

    private static void syncAvailabilityFromRequest(
            final PublishBoatForm form, final List<String> enabledDays, final List<String> availabilityRanges) {
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
                continue;
            }
            final DayOfWeek weekday = parseWeekday(parts[0]);
            if (weekday == null || !form.isDayEnabled(weekday)) {
                continue;
            }
            try {
                final LocalTime start = LocalTime.parse(parts[1]);
                final LocalTime end = LocalTime.parse(parts[2]);
                form.getAvailabilityFor(weekday).add(TimeRange.of(start, end));
            } catch (final DateTimeParseException | IllegalArgumentException ignored) {
                // Validated against the draft below.
            }
        }
    }

    private void applyDraftValidation(final PublishBoatForm form, final BindingResult errors, final Locale locale) {
        final PublicationDraft draft = toDraft(form, null);
        final Map<String, String> validationErrors = itemService.validatePublicationDraft(draft);
        for (final Map.Entry<String, String> entry : validationErrors.entrySet()) {
            errors.rejectValue(entry.getKey(), entry.getValue(), new Object[] {dayLabel(locale, null)}, null);
        }
    }

    private PublicationDraft toDraft(final PublishBoatForm form, final User currentUser) {
        return new PublicationDraft(
                currentUser == null ? null : currentUser.getGivenName(),
                currentUser == null ? null : currentUser.getLastName(),
                currentUser == null ? null : currentUser.getEmail(),
                currentUser == null || currentUser.getPreferredLanguage() == null
                        ? null
                        : currentUser.getPreferredLanguage().getPersistenceCode(),
                parseIntOrNull(form.getItemTypeId()),
                form.getTitle() == null ? null : form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                parseIntOrNull(form.getPricePerHour()),
                parseIntOrNull(form.getCapacity()),
                parseMaxWeight(form.getMaxWeight()),
                form.getDifficultyLevel(),
                parseIntOrNull(form.getLocationOptionId()),
                buildAvailabilitySlots(form),
                buildImageUploads(form));
    }

    private static List<GalleryImageUpload> buildImageUploads(final PublishBoatForm form) {
        if (!form.hasUploadedImages()) {
            return List.of();
        }
        final List<GalleryImageUpload> uploads = new ArrayList<>();
        for (final UploadedImage image : form.getUploadedImages()) {
            uploads.add(new GalleryImageUpload(null, image.getContentType(), image.getData()));
        }
        return uploads;
    }

    private static Integer parseIntOrNull(final String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (final NumberFormatException e) {
            return null;
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
        for (final Map.Entry<DayOfWeek, TimeRangeList> entry :
                form.getAvailabilityByWeekday().entrySet()) {
            final TimeRangeList dayRanges = entry.getValue();
            if (dayRanges == null || dayRanges.isEmpty()) {
                continue;
            }
            for (final TimeRange range : dayRanges) {
                if (range.getStart() == null || range.getEnd() == null) {
                    continue;
                }
                final ItemAvailability availability = new ItemAvailability();
                availability.setWeekday(entry.getKey());
                availability.setStartTime(range.getStart());
                availability.setEndTime(range.getEnd());
                availabilities.add(availability);
            }
        }
        return availabilities;
    }

    private void addSummaryData(final ModelAndView mav, final PublishBoatForm form, final Locale locale) {
        mav.addObject("availabilitySummary", buildAvailabilitySummary(form, locale));
        mav.addObject("uploadedImagePreviewUrls", buildUploadedImagePreviewUrls(form));
        mav.addObject("selectedLocationName", resolveLocationName(form.getLocationOptionId()));
    }

    private String resolveLocationName(final String locationOptionId) {
        if (!StringUtils.hasText(locationOptionId)) {
            return "";
        }
        try {
            final int selectedId = Integer.parseInt(locationOptionId.trim());
            return itemService.listLocationOptions().stream()
                    .filter(option -> option.getId() != null && option.getId() == selectedId)
                    .map(option -> option.getName() == null ? "" : option.getName())
                    .findFirst()
                    .orElse("");
        } catch (final NumberFormatException exception) {
            return "";
        }
    }

    private static List<String> buildUploadedImagePreviewUrls(final PublishBoatForm form) {
        final List<String> urls = new ArrayList<>();
        for (int i = 0; i < form.getUploadedImageCount(); i++) {
            urls.add(PUBLISH_PREVIEW_IMAGE_PATH + "/" + i);
        }
        return urls;
    }

    private static void appendUploadedImagesIfPresent(final PublishBoatForm form, final BindingResult errors) {
        final List<MultipartFile> uploaded = form.getFiles();
        if (uploaded == null || uploaded.isEmpty()) {
            return;
        }
        for (final MultipartFile file : uploaded) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (form.getUploadedImageCount() >= MAX_GALLERY_IMAGES) {
                errors.rejectValue("files", "publish.validation.images.count");
                return;
            }
            final String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType) || !contentType.regionMatches(true, 0, "image/", 0, 6)) {
                continue;
            }
            try {
                final byte[] imageBytes = file.getBytes();
                if (imageBytes.length > 0) {
                    form.appendUploadedImage(imageBytes, contentType);
                }
            } catch (final IOException ignored) {
                // Skip unreadable file.
            }
        }
    }

    private static List<Integer> parseOrderCsv(final String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        final List<Integer> result = new ArrayList<>();
        for (final String token : Arrays.asList(csv.split(","))) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (final NumberFormatException ignored) {
                return List.of();
            }
        }
        return result;
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
                sb.append(formatDisplayTime(sortedSlots.get(i).getStart()))
                        .append(" - ")
                        .append(formatDisplayTime(sortedSlots.get(i).getEnd()));
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
        return sb.append("]").toString();
    }

    private static String formatTime(final LocalTime time) {
        return time == null ? null : time.toString();
    }

    private static String formatDisplayTime(final LocalTime time) {
        final String formatted = formatTime(time);
        return formatted == null ? null : formatted + " hs";
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

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
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
