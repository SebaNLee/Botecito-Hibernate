package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.EditDao;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EditServiceImpl implements EditService {

    private final EditDao editDao;
    private final DetailService detailService;

    @Override
    @Transactional
    public boolean edit(
            final int itemId,
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final Integer difficulty,
            final int locationOptionId,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final Version current = detailService.getItemDetail(itemId, 1, ownerId).getLatestVersion();
        final List<AvailabilityWindow> filteredAvailabilities = filterAvailabilities(availabilities);
        final List<ImageUpload> filteredImages = filterImages(images);

        if (!hasChanges(
                current,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                weight,
                difficulty,
                locationOptionId,
                filteredAvailabilities,
                filteredImages)) {
            return false;
        }

        if (editDao.itemHasBookings(itemId)) {
            editDao.edit(
                    itemId,
                    typeId,
                    title,
                    description,
                    pricePerHour,
                    capacityPeople,
                    weight,
                    difficulty,
                    locationOptionId,
                    filteredAvailabilities,
                    filteredImages);
        } else {
            editDao.overwrite(
                    current.getId(),
                    typeId,
                    title,
                    description,
                    pricePerHour,
                    capacityPeople,
                    weight,
                    difficulty,
                    locationOptionId,
                    filteredAvailabilities,
                    filteredImages);
        }
        return true;
    }

    private static boolean hasChanges(
            final Version current,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final Integer difficulty,
            final int locationOptionId,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        if (current.getType().getId() != typeId) {
            return true;
        }
        if (!Objects.equals(normalize(current.getTitle()), normalize(title))) {
            return true;
        }
        if (!Objects.equals(normalize(current.getDescription()), normalize(description))) {
            return true;
        }
        if (current.getPrice().intValue() != pricePerHour) {
            return true;
        }
        if (!Objects.equals(current.getCapacity(), capacityPeople)) {
            return true;
        }
        if (!Objects.equals(current.getWeight(), weight)) {
            return true;
        }
        if (!Objects.equals(current.getDifficulty(), difficulty)) {
            return true;
        }
        if (current.getLocation().getId() != locationOptionId) {
            return true;
        }
        if (!sameAvailabilities(current.getAvailabilities(), availabilities)) {
            return true;
        }
        return !sameImages(current, images);
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean sameAvailabilities(
            final List<Availability> current, final List<AvailabilityWindow> submitted) {
        final Set<String> currentKeys = new TreeSet<>();
        if (current != null) {
            for (final Availability availability : current) {
                if (availability.getWeekday() == null
                        || availability.getStartTime() == null
                        || availability.getEndTime() == null) {
                    continue;
                }
                currentKeys.add(availabilityKey(
                        DayOfWeek.valueOf(availability.getWeekday().name()),
                        availability.getStartTime(),
                        availability.getEndTime()));
            }
        }
        final Set<String> submittedKeys = new TreeSet<>();
        for (final AvailabilityWindow window : submitted) {
            submittedKeys.add(availabilityKey(window.getWeekday(), window.getStartTime(), window.getEndTime()));
        }
        return currentKeys.equals(submittedKeys);
    }

    private static String availabilityKey(
            final DayOfWeek weekday, final java.time.LocalTime start, final java.time.LocalTime end) {
        return weekday.name() + "|" + start + "|" + end;
    }

    private static boolean sameImages(final Version current, final List<ImageUpload> submitted) {
        if (submitted.stream().anyMatch(upload -> !upload.isExisting())) {
            return false;
        }
        return currentImageIds(current)
                .equals(submitted.stream().map(ImageUpload::getExistingImageId).toList());
    }

    private static List<Integer> currentImageIds(final Version current) {
        if (current.getMedia() == null || current.getMedia().isEmpty()) {
            return List.of();
        }
        return current.getMedia().stream()
                .sorted(Comparator.comparingInt(m -> m.getId().getIndex()))
                .map(Media::getImage)
                .filter(Objects::nonNull)
                .map(Image::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<AvailabilityWindow> filterAvailabilities(final List<AvailabilityWindow> windows) {
        if (windows == null) {
            return List.of();
        }
        return windows.stream()
                .filter(w -> w != null && w.getWeekday() != null && w.getStartTime() != null && w.getEndTime() != null)
                .toList();
    }

    private static List<ImageUpload> filterImages(final List<ImageUpload> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        final List<ImageUpload> filtered = new ArrayList<>();
        for (final ImageUpload upload : images) {
            if (upload == null) {
                continue;
            }
            if (upload.isExisting()) {
                filtered.add(upload);
                continue;
            }
            if (upload.getData() != null && upload.getData().length > 0) {
                filtered.add(upload);
            }
        }
        return filtered;
    }
}
