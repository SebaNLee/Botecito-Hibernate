package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

public final class PublishWizardMapping {

    private PublishWizardMapping() {}

    public static List<AvailabilityWindow> toAvailabilityWindows(final PublishBoatForm form) {
        final List<AvailabilityWindow> availabilities = new ArrayList<>();
        for (final PublishBoatForm.AvailabilityRangeBinding range : form.getAvailabilityRanges()) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                continue;
            }
            final AvailabilityWindow window = new AvailabilityWindow();
            window.setWeekday(range.getWeekday());
            window.setStartTime(range.getStartTime());
            window.setEndTime(range.getEndTime());
            availabilities.add(window);
        }
        return availabilities;
    }

    public static List<ImageUpload> toPublishImageUploads(final PublishBoatForm form) {
        final List<MultipartFile> uploaded = form.getFiles();
        if (uploaded == null || uploaded.isEmpty()) {
            return List.of();
        }
        final List<ImageUpload> uploads = new ArrayList<>();
        for (final MultipartFile file : uploaded) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                final byte[] bytes = file.getBytes();
                if (bytes.length > 0) {
                    uploads.add(ImageUpload.ofNew(bytes, file.getContentType()));
                }
            } catch (final IOException ignored) {
                // skip unreadable file
            }
        }
        return uploads;
    }

    public static List<ImageUpload> toEditImageUploads(final PublishBoatForm form) {
        final String galleryOrder = form.getGalleryOrder();
        if (galleryOrder == null || galleryOrder.isBlank()) {
            return List.of();
        }
        final List<MultipartFile> uploaded = form.getFiles() == null ? List.of() : form.getFiles();
        int newFileIndex = 0;
        final List<ImageUpload> images = new ArrayList<>();
        for (final String token : galleryOrder.split(",")) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("e:")) {
                images.add(ImageUpload.ofExisting(Integer.parseInt(trimmed.substring(2))));
                continue;
            }
            if (trimmed.startsWith("n:")) {
                while (newFileIndex < uploaded.size()) {
                    final MultipartFile file = uploaded.get(newFileIndex++);
                    if (file == null || file.isEmpty()) {
                        continue;
                    }
                    try {
                        final byte[] bytes = file.getBytes();
                        if (bytes.length > 0) {
                            images.add(ImageUpload.ofNew(bytes, file.getContentType()));
                        }
                    } catch (final IOException ignored) {
                        // skip unreadable file
                    }
                    break;
                }
            }
        }
        return images;
    }

    public static int countGallerySlots(final PublishBoatForm form) {
        final String galleryOrder = form.getGalleryOrder();
        if (galleryOrder == null || galleryOrder.isBlank()) {
            return 0;
        }
        int count = 0;
        for (final String token : galleryOrder.split(",")) {
            if (!token.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static void addAvailabilityEditorData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject("existingSlotsJson", toExistingSlotsJson(form));
        mav.addObject("enabledWeekdays", buildEnabledWeekdaysModel(form));
    }

    public static boolean hasErrorsOutsideAvailability(final BindingResult errors) {
        return errors.getFieldErrors().stream().anyMatch(error -> !isAvailabilityError(error.getField()));
    }

    public static Map<String, Boolean> buildEnabledWeekdaysModel(final PublishBoatForm form) {
        final Map<String, Boolean> model = new LinkedHashMap<>();
        for (final DayOfWeek weekday : DayOfWeek.values()) {
            model.put(weekday.name(), form.getEnabledDays().contains(weekday));
        }
        return model;
    }

    public static String toExistingSlotsJson(final PublishBoatForm form) {
        if (form.getAvailabilityRanges() == null || form.getAvailabilityRanges().isEmpty()) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (final PublishBoatForm.AvailabilityRangeBinding range : form.getAvailabilityRanges()) {
            if (range == null
                    || range.getWeekday() == null
                    || range.getStartTime() == null
                    || range.getEndTime() == null) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            sb.append("{\"weekday\":\"")
                    .append(range.getWeekday().name())
                    .append("\",\"startTime\":\"")
                    .append(range.getStartTime())
                    .append("\",\"endTime\":\"")
                    .append(range.getEndTime())
                    .append("\"}");
            first = false;
        }
        return sb.append("]").toString();
    }

    private static boolean isAvailabilityError(final String field) {
        if (field == null) {
            return false;
        }
        return field.startsWith("availability") || "availabilityByWeekday".equals(field);
    }
}
