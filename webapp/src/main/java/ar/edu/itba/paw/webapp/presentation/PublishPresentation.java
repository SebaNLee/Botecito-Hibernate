package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingDecisionBatch;
import ar.edu.itba.paw.services.GalleryImageUpload;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.PublicationDraft;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.util.BookingDisplayFormatter;
import ar.edu.itba.paw.services.utils.TimeRange;
import ar.edu.itba.paw.services.utils.TimeRangeList;
import ar.edu.itba.paw.webapp.controller.support.ItemImageUtils;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
import ar.edu.itba.paw.webapp.form.EditPublicationForm;
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
import java.util.Objects;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class PublishPresentation {

    public static final int MAX_GALLERY_IMAGES = 3;

    private static final Map<DayOfWeek, String> WEEKDAY_LABELS = Map.of(
            DayOfWeek.MONDAY, "Lunes",
            DayOfWeek.TUESDAY, "Martes",
            DayOfWeek.WEDNESDAY, "Miercoles",
            DayOfWeek.THURSDAY, "Jueves",
            DayOfWeek.FRIDAY, "Viernes",
            DayOfWeek.SATURDAY, "Sabado",
            DayOfWeek.SUNDAY, "Domingo");

    private final ItemService itemService;
    private final UserService userService;
    private final MessageSource messageSource;

    public ModelAndView publishStepOne(final PublishBoatForm form) {
        return new ModelAndView("publish");
    }

    public ModelAndView publishStepOneSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (!errors.hasFieldErrors("files")) {
            appendUploadedImagesIfPresent(form, errors);
        }
        form.setFiles(new ArrayList<>());

        if (errors.hasErrors()) {
            return new ModelAndView("publish");
        }

        return new ModelAndView("redirect:/publish/availability");
    }

    public ModelAndView publishImagesUpload(final PublishBoatForm form, final BindingResult errors) {
        appendUploadedImagesIfPresent(form, errors);
        form.setFiles(new ArrayList<>());
        return new ModelAndView("redirect:/publish");
    }

    public ModelAndView publishImagesRemove(final PublishBoatForm form, final int index) {
        form.removeUploadedImageAt(index);
        return new ModelAndView("redirect:/publish");
    }

    public ModelAndView publishImagesReorder(final PublishBoatForm form, final String order) {
        final List<Integer> parsed = parseOrderCsv(order);
        form.reorderUploadedImages(parsed);
        return new ModelAndView("redirect:/publish");
    }

    public ModelAndView publishStepTwo(final PublishBoatForm form) {
        if (!isStepOneComplete(form)) {
            return new ModelAndView("redirect:/publish");
        }

        final ModelAndView mav = new ModelAndView("publish-availability");
        addAvailabilityEditorData(mav, form);
        return mav;
    }

    public ModelAndView publishStepTwoSubmit(
            final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            final List<String> enabledDays,
            final List<String> availabilityRanges) {
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

    public ModelAndView publishStepThree(final PublishBoatForm form, final Locale locale) {
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

    public ResponseEntity<byte[]> publishPreviewImage(final PublishBoatForm form, final int index) {
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

    public ModelAndView publishStepThreeSubmit(
            final PublishBoatForm form,
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

        final Optional<Item> createdItem = itemService.createPublicationFromDraft(toDraft(form, currentUser));
        if (createdItem.isEmpty()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            errors.reject("publish.submit.persistenceError");
            addSummaryData(mav, form, locale);
            return mav;
        }

        sessionStatus.setComplete();
        return new ModelAndView(
                "redirect:/publish/success?itemId=" + createdItem.get().getId());
    }

    public ModelAndView publishSuccess(final HttpServletRequest request, final Integer itemId) {
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
        mav.addObject("availabilities", itemService.listAvailabilitiesByItemId(item.getId()));
        return mav;
    }

    public ModelAndView editPublicationForm(
            final int itemId, final HttpServletRequest request, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        final EditPublicationForm form = new EditPublicationForm();
        form.setTitle(item.get().getTitle());
        form.setDescription(item.get().getDescription());
        form.setPricePerHour(
                item.get().getPricePerHour() == null
                        ? ""
                        : String.valueOf(item.get().getPricePerHour()));
        form.setDifficultyLevel(item.get().getDifficultyLevel());
        form.setMarina(
                item.get().getLocationOptionId() == null
                        ? ""
                        : String.valueOf(item.get().getLocationOptionId()));

        return editPublicationModelAndView(item.get(), request).addObject("editForm", form);
    }

    public ModelAndView editPublicationSubmit(
            final int itemId,
            final EditPublicationForm form,
            final BindingResult errors,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats");
        }

        validateUploadedImage(form.getFile(), errors);
        final Integer parsedPrice =
                parseIntegerField(form.getPricePerHour(), "pricePerHour", "publish.validation.price.numeric", errors);
        final Integer parsedLocationOptionId =
                parseIntegerField(form.getMarina(), "marina", "publish.validation.location.invalid", errors);
        if (parsedLocationOptionId != null
                && itemService.listLocationOptions().stream()
                        .noneMatch(option -> parsedLocationOptionId.equals(option.getId()))) {
            errors.rejectValue("marina", "publish.validation.location.invalid");
        }

        if (errors.hasErrors()) {
            return editPublicationModelAndView(item.get(), request);
        }
        if (parsedPrice == null || parsedLocationOptionId == null) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }
        final int publicationPrice = parsedPrice;
        final int publicationLocationOptionId = parsedLocationOptionId;
        final MultipartFile file = form.getFile();
        final boolean hasNewPrimaryImage = file != null && !file.isEmpty();
        final byte[] primaryImageDataForChangeDetection = hasNewPrimaryImage ? new byte[] {0} : null;

        if (!itemService.hasPublicationChanges(
                itemId,
                form.getTitle() == null ? "" : form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                publicationPrice,
                form.getDifficultyLevel(),
                publicationLocationOptionId,
                primaryImageDataForChangeDetection)) {
            ToastSupport.success(redirectAttributes, "profile.publications.updated");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(itemId);
        if (!activeBookings.isEmpty() && !isConfirmedSnapshotEdit(request)) {
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        if (!allPendingBookingsHaveDecisions(activeBookings, request)) {
            errors.reject("editPublication.conflict.pending.required");
            return editPublicationModelAndView(item.get(), request).addObject("showEditConflictModal", true);
        }
        itemService.resolveEditConflict(itemId, buildDecisionBatch(activeBookings, request));

        final byte[] primaryImageData;
        if (hasNewPrimaryImage) {
            primaryImageData = readPrimaryImageBytes(Objects.requireNonNull(file, "file"), errors);
            if (errors.hasErrors()) {
                return editPublicationModelAndView(item.get(), request);
            }
        } else {
            primaryImageData = null;
        }

        if (!itemService.updatePublicationForOwner(
                itemId,
                currentUser.getId(),
                form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                publicationPrice,
                form.getDifficultyLevel(),
                publicationLocationOptionId,
                primaryImageData)) {
            errors.reject("publish.submit.persistenceError");
            return editPublicationModelAndView(item.get(), request);
        }

        ToastSupport.success(redirectAttributes, "profile.publications.updated");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView disablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty() || !itemService.setItemActiveForOwner(itemId, currentUser.getId(), false)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.disabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView enablePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty() || !itemService.setItemActiveForOwner(itemId, currentUser.getId(), true)) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.enabled");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public ModelAndView hardDeletePublication(final int itemId, final RedirectAttributes redirectAttributes) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Item> item = resolveOwnedItem(currentUser, itemId);
        if (item.isEmpty()) {
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        if (!itemService.deleteItemByIdForOwner(itemId, currentUser.getId())) {
            if (!Boolean.TRUE.equals(item.get().getActive())) {
                ToastSupport.error(redirectAttributes, "profile.publications.deleteBlockedByBookings");
                return new ModelAndView("redirect:/my-boats#my-publications");
            }
            ToastSupport.error(redirectAttributes, "profile.publications.error");
            return new ModelAndView("redirect:/my-boats#my-publications");
        }

        ToastSupport.success(redirectAttributes, "profile.publications.deleted");
        return new ModelAndView("redirect:/my-boats#my-publications");
    }

    public static List<String> buildUploadedImagePreviewUrls(final PublishBoatForm form) {
        final List<String> urls = new ArrayList<>();
        for (int i = 0; i < form.getUploadedImageCount(); i++) {
            urls.add("/publish/preview-image" + "/" + i);
        }
        return urls;
    }

    public static Map<String, String> buildItemTypeOptions() {
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

    public static Map<String, String> buildCapacityOptions() {
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

    public static Map<String, String> buildDifficultyOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("1", "1 - Principiante");
        options.put("2", "2 - Basico");
        options.put("3", "3 - Intermedio");
        options.put("4", "4 - Avanzado");
        options.put("5", "5 - Experto");
        return options;
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
            final String[] parts = serializedRange.split("\\|", -1);
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
                if (i > 0) {
                    sb.append(", ");
                }
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

    private Optional<Item> resolveOwnedItem(final User currentUser, final int itemId) {
        return itemService.findItemByIdForOwner(itemId, currentUser.getId());
    }

    private ModelAndView editPublicationModelAndView(final Item item, final HttpServletRequest request) {
        final ModelAndView mav = new ModelAndView("edit-publication");
        final List<ItemBooking> activeBookings = itemService.listActiveBookingsByItemId(item.getId());
        final Map<Integer, String> guestNames = new LinkedHashMap<>();
        for (final ItemBooking booking : activeBookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            guestNames.put(
                    booking.getId(),
                    booking.getGuestId() == null
                            ? ""
                            : itemService
                                    .findUserById(booking.getGuestId())
                                    .map(User::getName)
                                    .orElse(""));
        }
        final Map<Integer, String> startLabels = new LinkedHashMap<>();
        final Map<Integer, String> friendlyDates = new LinkedHashMap<>();
        final Map<Integer, String> friendlyTimeRanges = new LinkedHashMap<>();
        final Map<Integer, String> friendlyPrices = new LinkedHashMap<>();
        final Map<Integer, String> statusCodes = new LinkedHashMap<>();
        final Integer pricePerHour = item.getPricePerHour();
        for (final ItemBooking booking : activeBookings) {
            if (booking == null || booking.getId() == null) {
                continue;
            }
            final int id = booking.getId();
            startLabels.put(id, BookingDisplayFormatter.formatStartLabel(booking.getStartTime()));
            friendlyDates.put(id, BookingDisplayFormatter.formatFriendlyDate(booking.getStartTime()));
            friendlyTimeRanges.put(
                    id, BookingDisplayFormatter.formatFriendlyTimeRange(booking.getStartTime(), booking.getEndTime()));
            friendlyPrices.put(
                    id,
                    BookingDisplayFormatter.formatFriendlyTotalPrice(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            statusCodes.put(
                    id,
                    booking.getState() == null ? "" : BookingDisplayFormatter.statusMessageCode(booking.getState()));
        }
        mav.addObject("item", item);
        mav.addObject("activeEditBookings", activeBookings);
        mav.addObject("editBookingGuests", guestNames);
        mav.addObject("editBookingStartLabels", startLabels);
        mav.addObject("editBookingFriendlyDates", friendlyDates);
        mav.addObject("editBookingFriendlyTimeRanges", friendlyTimeRanges);
        mav.addObject("editBookingFriendlyPrices", friendlyPrices);
        mav.addObject("editBookingStatusCodes", statusCodes);
        mav.addObject(
                "itemImageUrl", ItemImageUtils.resolveImageUrl(itemService, item.getId(), request.getContextPath()));
        return mav;
    }

    private static BookingDecisionBatch buildDecisionBatch(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        final List<BookingDecisionBatch.Decision> decisions = new ArrayList<>();
        for (final ItemBooking booking : activeBookings) {
            if (booking == null
                    || booking.getState() != BookingState.BOOKING_PENDING
                    || booking.getHostDecisionToken() == null) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if ("accept".equals(decision)) {
                decisions.add(new BookingDecisionBatch.Decision(
                        booking.getHostDecisionToken(), BookingState.BOOKING_CONFIRMED));
            } else if ("decline".equals(decision)) {
                decisions.add(new BookingDecisionBatch.Decision(
                        booking.getHostDecisionToken(), BookingState.BOOKING_REJECTED));
            }
        }
        return new BookingDecisionBatch(decisions);
    }

    private static boolean allPendingBookingsHaveDecisions(
            final List<ItemBooking> activeBookings, final HttpServletRequest request) {
        for (final ItemBooking booking : activeBookings) {
            if (booking == null || booking.getState() != BookingState.BOOKING_PENDING) {
                continue;
            }
            final String decision = request.getParameter("bookingDecision_" + booking.getId());
            if (!"accept".equals(decision) && !"decline".equals(decision)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isConfirmedSnapshotEdit(final HttpServletRequest request) {
        return "true".equals(request.getParameter("confirmEditWithSnapshots"));
    }

    private static Integer parseIntegerField(
            final String rawValue, final String fieldName, final String errorCode, final BindingResult errors) {
        if (rawValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (final NumberFormatException e) {
            errors.rejectValue(fieldName, errorCode);
            return null;
        }
    }

    private static void validateUploadedImage(final MultipartFile file, final BindingResult errors) {
        if (file == null || file.isEmpty()) {
            return;
        }
        final String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            errors.rejectValue("file", "editPublication.validation.image.type");
        }
    }

    private static byte[] readPrimaryImageBytes(final MultipartFile uploadedFile, final BindingResult errors) {
        try {
            return uploadedFile.getBytes();
        } catch (final IOException e) {
            errors.rejectValue("file", "editPublication.validation.image.read");
            return null;
        }
    }
}
