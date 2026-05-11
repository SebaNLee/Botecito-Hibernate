package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ImageUpload;
import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.PublishContent;
import ar.edu.itba.paw.models.nuevo.PublishItem;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.services.nuevo.PublishService;
import ar.edu.itba.paw.services.nuevo.SelectorsInterface;
import ar.edu.itba.paw.services.nuevo.UserService;
import ar.edu.itba.paw.services.utils.TimeRange;
import ar.edu.itba.paw.services.utils.TimeRangeList;
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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class PublishPresentation {

    private final PublishService publishService;
    private final UserService userService;
    private final SelectorsInterface selectorsInterface;
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
            final PublishBoatForm form,
            final BindingResult errors,
            final Locale locale,
            final SessionStatus sessionStatus) {
        final UserModel currentUser = currentAuthenticatedUser();
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

        final Optional<PublishItem> createdItem = publishService.create(toDraft(form), currentUser.getId());
        if (createdItem.isEmpty()) {
            final ModelAndView mav = new ModelAndView("publish-contact");
            errors.reject("publish.submit.persistenceError");
            addSummaryData(mav, form, locale);
            return mav;
        }

        sessionStatus.setComplete();
        return new ModelAndView("redirect:/publish/success?itemId=" + createdItem.get().getId());
    }

    public ModelAndView publishSuccess(final HttpServletRequest request, final Integer itemId) {
        final UserModel currentUser = currentAuthenticatedUser();
        if (currentUser == null || itemId == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<PublishItem> item = publishService.findById(itemId);
        if (item.isEmpty()) {
            return new ModelAndView("redirect:/publish");
        }
        if (item.get().getOwnerId() != currentUser.getId()) {
            return new ModelAndView("redirect:/403");
        }

        final ModelAndView mav = new ModelAndView("publish-success");
        mav.addObject("item", item.get());
        mav.addObject("itemImageUrl", resolveImageUrl(item.get(), request.getContextPath()));
        mav.addObject("availabilities", publishService.listAvailabilities(item.get().getId()));
        return mav;
    }

    public Map<String, String> buildItemTypeOptions() {
        final List<ItemTypeModel> types = selectorsInterface.getItemTypeOptions();
        final Map<String, String> options = new LinkedHashMap<>();
        for (final ItemTypeModel type : types) {
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
            try {
                final LocalTime start = LocalTime.parse(parts[1]);
                final LocalTime end = LocalTime.parse(parts[2]);
                form.getAvailabilityFor(weekday).add(TimeRange.of(start, end));
            } catch (final DateTimeParseException | IllegalArgumentException ignored) {
            }
        }
    }

    private void applyDraftValidation(final PublishBoatForm form, final BindingResult errors, final Locale locale) {
        final PublishContent draft = toDraft(form);
        final Map<String, String> validationErrors = publishService.validate(draft);
        for (final Map.Entry<String, String> entry : validationErrors.entrySet()) {
            errors.rejectValue(entry.getKey(), entry.getValue(), new Object[] {dayLabel(locale, null)}, null);
        }
    }

    private PublishContent toDraft(final PublishBoatForm form) {
        return new PublishContent(
                parseIntOrNull(form.getItemTypeId()),
                form.getTitle() == null ? null : form.getTitle().trim(),
                form.getDescription() == null ? "" : form.getDescription().trim(),
                parseIntOrNull(form.getPricePerHour()),
                parseIntOrNull(form.getCapacity()),
                parseMaxWeight(form.getMaxWeight()),
                form.getDifficultyLevel(),
                parseIntOrNull(form.getLocationOptionId()),
                buildAvailabilityWindows(form),
                buildImageUploads(form));
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
                final AvailabilityWindow window = new AvailabilityWindow();
                window.setWeekday(entry.getKey());
                window.setStartTime(range.getStart());
                window.setEndTime(range.getEnd());
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
        try {
            final int selectedId = Integer.parseInt(locationOptionId.trim());
            return selectorsInterface.getLocationOptions().stream()
                    .filter(option -> option.getId() != null && option.getId() == selectedId)
                    .map(option -> option.getName() == null ? "" : option.getName())
                    .findFirst()
                    .orElse("");
        } catch (final NumberFormatException exception) {
            return "";
        }
    }

    private List<String> buildAvailabilitySummary(final PublishBoatForm form, final Locale locale) {
        final List<String> summary = new ArrayList<>();
        for (final DayOfWeek weekday : DayOfWeek.values()) {
            final TimeRangeList daySlots = form.getAvailabilityFor(weekday);
            if (daySlots == null || daySlots.isEmpty()) {
                continue;
            }
            final List<TimeRange> sortedSlots = new ArrayList<>(daySlots);
            sortedSlots.sort(Comparator.comparing(TimeRange::getStart));

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

    private UserModel currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    private static String resolveImageUrl(final PublishItem item, final String contextPath) {
        final String prefix = contextPath == null ? "" : contextPath;
        final Integer coverImageId = item.getCoverImageId();
        if (coverImageId != null) {
            return prefix + "/image/" + coverImageId;
        }
        return prefix + "/css/boat-placeholder.svg";
    }
}
