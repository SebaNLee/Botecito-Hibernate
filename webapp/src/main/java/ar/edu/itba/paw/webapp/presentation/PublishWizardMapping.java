package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    public static void addAvailabilityEditorData(final ModelAndView mav, final PublishBoatForm form) {
        mav.addObject("enabledWeekdays", buildEnabledWeekdaysModel(form));
    }

    public static PublishBoatForm fromVersion(final Version version) {
        final PublishBoatForm form = new PublishBoatForm();
        form.setTitle(version.getTitle());
        form.setDescription(version.getDescription() == null ? "" : version.getDescription());
        form.setItemTypeId(version.getType().getId());
        form.setPricePerHour(version.getPrice().intValue());
        form.setCapacity(version.getCapacity());
        form.setWeight(version.getWeight());
        form.setDifficulty(version.getDifficulty());
        form.setLocationOptionId(version.getLocation().getId());

        final LinkedHashSet<DayOfWeek> enabledDays = new LinkedHashSet<>();
        final List<PublishBoatForm.AvailabilityRangeBinding> ranges = new ArrayList<>();
        if (version.getAvailabilities() != null) {
            for (final Availability availability : version.getAvailabilities()) {
                if (availability.getWeekday() == null
                        || availability.getStartTime() == null
                        || availability.getEndTime() == null) {
                    continue;
                }
                enabledDays.add(DayOfWeek.valueOf(availability.getWeekday().name()));
                final PublishBoatForm.AvailabilityRangeBinding range = new PublishBoatForm.AvailabilityRangeBinding();
                range.setWeekday(DayOfWeek.valueOf(availability.getWeekday().name()));
                range.setStartTime(availability.getStartTime());
                range.setEndTime(availability.getEndTime());
                ranges.add(range);
            }
        }
        form.setEnabledDays(new ArrayList<>(enabledDays));
        form.setAvailabilityRanges(ranges);
        return form;
    }

    public static List<EditGalleryImageSeed> buildEditGallerySeeds(final Version version, final String contextPath) {
        if (version.getMedia() == null || version.getMedia().isEmpty()) {
            return List.of();
        }
        final List<EditGalleryImageSeed> images = new ArrayList<>();
        version.getMedia().stream()
                .sorted(Comparator.comparingInt(m -> m.getId().getIndex()))
                .forEach(media -> {
                    if (media.getImage() == null || media.getImage().getId() == null) {
                        return;
                    }
                    images.add(new EditGalleryImageSeed(
                            media.getImage().getId(),
                            contextPath + "/image/" + media.getImage().getId()));
                });
        return images;
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

    private static boolean isAvailabilityError(final String field) {
        if (field == null) {
            return false;
        }
        return field.startsWith("availability") || "availabilityByWeekday".equals(field);
    }
}
