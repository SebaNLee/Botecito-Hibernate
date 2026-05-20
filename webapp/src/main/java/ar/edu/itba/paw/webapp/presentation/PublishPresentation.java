package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.services.PublishService;
import ar.edu.itba.paw.services.SelectorsService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class PublishPresentation {

    private final PublishService publishService;
    private final SelectorsService selectorsInterface;
    private final MessageSource messageSource;

    public ModelAndView publishStepOne(final PublishBoatForm form) {
        return new ModelAndView("publish");
    }

    public ModelAndView publishStepOneSubmit(final PublishBoatForm form, final BindingResult errors) {
        if (errors.hasErrors()) {
            return new ModelAndView("publish");
        }
        return new ModelAndView("redirect:/publish/availability");
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

    public ModelAndView publishStepThreeSubmit(
            final BotecitoUserDetails principal,
            final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            final SessionStatus sessionStatus) {
        if (principal == null) {
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

        final Optional<Version> createdVersion = publishService.create(
                principal.getId(),
                parseIntOrNull(form.getItemTypeId()),
                form.getTitle() == null ? null : form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                parseIntOrNull(form.getPricePerHour()),
                parseIntOrNull(form.getCapacity()),
                parseWeight(form.getWeight()),
                form.getDifficulty(),
                parseIntOrNull(form.getLocationOptionId()),
                buildAvailabilityWindows(form),
                buildImageUploads(form));
        if (createdVersion.isEmpty()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            errors.reject("publish.submit.persistenceError");
            addSummaryData(mav, form, locale);
            return mav;
        }

        sessionStatus.setComplete();
        return new ModelAndView("redirect:/publish/success?itemId="
                + createdVersion.get().getItem().getId());
    }

    public ModelAndView publishSuccess(
            final BotecitoUserDetails principal, final HttpServletRequest request, final Integer itemId) {
        if (principal == null || itemId == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<Version> version = publishService.findByIdForHost(itemId, principal.getId());
        if (version.isEmpty()) {
            return new ModelAndView("redirect:/publish");
        }

        final ModelAndView mav = new ModelAndView("publish-success");
        mav.addObject("item", version.get());
        mav.addObject("itemImageUrl", resolveImageUrl(version.get(), request.getContextPath()));
        mav.addObject(
                "availabilities",
                publishService.listAvailabilities(version.get().getItem().getId()));
        return mav;
    }

    public Map<String, String> buildItemTypeOptions() {
        final List<ItemType> types = selectorsInterface.getItemTypeOptions();
        final Map<String, String> options = new LinkedHashMap<>();
        for (final ItemType type : types) {
            final String id = type.getId() == null ? "" : String.valueOf(type.getId());
            final String name = type.getName() == null ? "" : type.getName();
            options.put(id, name);
        }
        return options;
    }

    public Map<String, String> buildDifficultyOptions() {
        return selectorsInterface.getDifficultyOptions();
    }

    private static boolean isStepOneComplete(final PublishBoatForm form) {
        return StringUtils.hasText(form.getTitle())
                && StringUtils.hasText(form.getLocationOptionId())
                && StringUtils.hasText(form.getCapacity())
                && StringUtils.hasText(form.getItemTypeId())
                && StringUtils.hasText(form.getPricePerHour());
    }

    private static BigDecimal parseWeight(final String weight) {
        if (!StringUtils.hasText(weight)) {
            return null;
        }
        return new BigDecimal(weight.trim());
    }

    private static void addAvailabilityEditorData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject("existingSlotsJson", buildExistingSlotsJson(form));
        mav.addObject("enabledWeekdays", buildEnabledWeekdaysModel(form));
    }

    private static Map<String, Boolean> buildEnabledWeekdaysModel(final PublishBoatForm form) {
        final Map<String, Boolean> model = new LinkedHashMap<>();
        for (final DayOfWeek weekday : DayOfWeek.values()) {
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
            final LocalTime start = parseLocalTimeOrNull(parts[1]);
            final LocalTime end = parseLocalTimeOrNull(parts[2]);
            if (start != null && end != null) {
                form.getAvailabilityFor(weekday).add(PublishBoatForm.TimeSlot.of(start, end));
            }
        }
    }

    private void applyDraftValidation(final PublishBoatForm form, final BindingResult errors, final Locale locale) {
        final Map<String, String> validationErrors = publishService.validate(
                form.getTitle() == null ? null : form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                parseIntOrNull(form.getPricePerHour()),
                parseIntOrNull(form.getCapacity()),
                parseWeight(form.getWeight()),
                form.getDifficulty(),
                parseIntOrNull(form.getLocationOptionId()),
                buildAvailabilityWindows(form),
                buildImageUploads(form));
        for (final Map.Entry<String, String> entry : validationErrors.entrySet()) {
            errors.rejectValue(entry.getKey(), entry.getValue(), new Object[] {dayLabel(locale, null)}, null);
        }
    }

    private static List<ImageUpload> buildImageUploads(final PublishBoatForm form) {
        if (!form.hasUploadedImages()) {
            return List.of();
        }
        final List<ImageUpload> uploads = new ArrayList<>();
        for (final PublishBoatForm.UploadedImage image : form.getUploadedImages()) {
            uploads.add(new ImageUpload(image.getData(), image.getContentType()));
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

    private static LocalTime parseLocalTimeOrNull(final String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (final DateTimeParseException e) {
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

    private static List<AvailabilityWindow> buildAvailabilityWindows(final PublishBoatForm form) {
        final List<AvailabilityWindow> availabilities = new ArrayList<>();
        for (final Map.Entry<DayOfWeek, List<PublishBoatForm.TimeSlot>> entry :
                form.getAvailabilityByWeekday().entrySet()) {
            final List<PublishBoatForm.TimeSlot> dayRanges = entry.getValue();
            if (dayRanges == null || dayRanges.isEmpty()) {
                continue;
            }
            for (final PublishBoatForm.TimeSlot slot : dayRanges) {
                final AvailabilityWindow window = new AvailabilityWindow();
                window.setWeekday(entry.getKey());
                window.setStartTime(slot.getStart());
                window.setEndTime(slot.getEnd());
                availabilities.add(window);
            }
        }
        return availabilities;
    }

    private void addSummaryData(final ModelAndView mav, final PublishBoatForm form, final Locale locale) {
        mav.addObject("availabilitySummary", buildAvailabilitySummary(form, locale));
        mav.addObject("selectedLocationName", resolveLocationName(form.getLocationOptionId()));
    }

    private String resolveLocationName(final String locationOptionId) {
        if (!StringUtils.hasText(locationOptionId)) {
            return "";
        }
        final Integer selectedId = parseIntOrNull(locationOptionId);
        if (selectedId == null) {
            return "";
        }
        return selectorsInterface.getLocationOptions().stream()
                .filter(option -> selectedId.equals(option.getId()))
                .map(option -> option.getName() == null ? "" : option.getName())
                .findFirst()
                .orElse("");
    }

    private List<String> buildAvailabilitySummary(final PublishBoatForm form, final Locale locale) {
        final List<String> summary = new ArrayList<>();
        for (final DayOfWeek weekday : DayOfWeek.values()) {
            final List<PublishBoatForm.TimeSlot> daySlots = form.getAvailabilityFor(weekday);
            if (daySlots == null || daySlots.isEmpty()) {
                continue;
            }
            final List<PublishBoatForm.TimeSlot> sortedSlots = new ArrayList<>(daySlots);
            sortedSlots.sort(Comparator.comparing(PublishBoatForm.TimeSlot::getStart));

            final StringBuilder sb = new StringBuilder(dayLabel(locale, weekday)).append(": ");
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
        for (final DayOfWeek weekday : DayOfWeek.values()) {
            final List<PublishBoatForm.TimeSlot> dayRanges = form.getAvailabilityFor(weekday);
            if (dayRanges == null || dayRanges.isEmpty()) {
                continue;
            }
            for (final PublishBoatForm.TimeSlot slot : dayRanges) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("{\"weekday\":\"")
                        .append(weekday.name())
                        .append("\",\"startTime\":\"")
                        .append(formatTime(slot.getStart()))
                        .append("\",\"endTime\":\"")
                        .append(formatTime(slot.getEnd()))
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

    private static String resolveImageUrl(final Version version, final String contextPath) {
        final String prefix = contextPath == null ? "" : contextPath;
        if (version.getMedia() != null && !version.getMedia().isEmpty()) {
            final Integer coverImageId = version.getMedia().get(0).getImage().getId();
            if (coverImageId != null) {
                return prefix + "/image/" + coverImageId;
            }
        }
        return prefix + "/css/boat-placeholder.svg";
    }
}
