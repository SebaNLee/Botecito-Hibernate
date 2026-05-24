package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.entity.WeekdayEnum;
import ar.edu.itba.paw.persistence.EditDao;
import ar.edu.itba.paw.persistence.PublishDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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
    private final PublishDao publishDao;
    private final ItemService itemService;

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
        final Version current = itemService.requireOwnedItem(itemId, ownerId).getLatestVersion();
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
            createNewVersion(
                    current,
                    typeId,
                    title,
                    description,
                    pricePerHour,
                    capacityPeople,
                    weight,
                    Objects.requireNonNull(difficulty),
                    locationOptionId,
                    filteredAvailabilities,
                    filteredImages);
        } else {
            overwriteVersion(
                    current.getId(),
                    typeId,
                    title,
                    description,
                    pricePerHour,
                    capacityPeople,
                    weight,
                    Objects.requireNonNull(difficulty),
                    locationOptionId,
                    filteredAvailabilities,
                    filteredImages);
        }
        return true;
    }

    private void createNewVersion(
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
        final Version version = publishDao.persistVersion(buildNewVersion(
                current,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                weight,
                difficulty,
                locationOptionId,
                LocalDateTime.now()));
        publishDao.flush();
        persistAvailabilities(version, availabilities);
        persistMedia(version, images, allowedExistingImageIds(current));
    }

    private void overwriteVersion(
            final int versionId,
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
        final Version version = editDao.findVersionById(versionId)
                .orElseThrow(() -> new IllegalStateException("Version not found: " + versionId));

        applyVersionFields(
                version,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                weight,
                difficulty,
                locationOptionId);

        editDao.removeVersionChildren(version);
        publishDao.flush();

        persistAvailabilities(version, availabilities);
        persistMedia(version, images, Set.of());
    }

    private void persistAvailabilities(final Version version, final List<AvailabilityWindow> availabilities) {
        for (final AvailabilityWindow window : availabilities) {
            publishDao.persistAvailability(buildAvailability(version, window));
        }
    }

    private void persistMedia(
            final Version version, final List<ImageUpload> images, final Set<Integer> allowedExistingImageIds) {
        for (int idx = 0; idx < images.size(); idx++) {
            final ImageUpload upload = images.get(idx);
            final Image image;
            if (upload.isExisting()) {
                if (!allowedExistingImageIds.isEmpty()
                        && !allowedExistingImageIds.contains(upload.getExistingImageId())) {
                    continue;
                }
                image = imageReference(upload.getExistingImageId());
            } else {
                image = publishDao.persistImage(buildNewImage(upload));
            }
            publishDao.persistMedia(buildMedia(version, image, idx));
        }
    }

    private static Version buildNewVersion(
            final Version current,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final Integer difficulty,
            final int locationOptionId,
            final LocalDateTime createdAt) {
        return Version.builder()
                .item(current.getItem())
                .type(itemTypeReference(typeId))
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(pricePerHour))
                .capacity(capacityPeople)
                .weight(weight)
                .difficulty(difficulty)
                .location(locationReference(locationOptionId))
                .timezone(current.getTimezone())
                .createdAt(createdAt)
                .build();
    }

    private static void applyVersionFields(
            final Version version,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final int weight,
            final Integer difficulty,
            final int locationOptionId) {
        version.setType(itemTypeReference(typeId));
        version.setTitle(title);
        version.setDescription(description);
        version.setPrice(BigDecimal.valueOf(pricePerHour));
        version.setCapacity(capacityPeople);
        version.setWeight(weight);
        version.setDifficulty(difficulty);
        version.setLocation(locationReference(locationOptionId));
    }

    private static Availability buildAvailability(final Version version, final AvailabilityWindow window) {
        final Availability availability = new Availability();
        availability.setVersion(version);
        availability.setWeekday(WeekdayEnum.valueOf(window.getWeekday().name()));
        availability.setStartTime(window.getStartTime());
        availability.setEndTime(window.getEndTime());
        return availability;
    }

    private static Image buildNewImage(final ImageUpload upload) {
        final Image image = new Image();
        image.setData(upload.getData());
        return image;
    }

    private static Media buildMedia(final Version version, final Image image, final int index) {
        final Media media = new Media();
        media.setId(new MediaId(version.getId(), index));
        media.setVersion(version);
        media.setImage(image);
        return media;
    }

    private static ItemType itemTypeReference(final int typeId) {
        final ItemType type = new ItemType();
        type.setId(typeId);
        return type;
    }

    private static Location locationReference(final int locationId) {
        final Location location = new Location();
        location.setId(locationId);
        return location;
    }

    private static Image imageReference(final int imageId) {
        final Image image = new Image();
        image.setId(imageId);
        return image;
    }

    private static Set<Integer> allowedExistingImageIds(final Version version) {
        final Set<Integer> allowed = new HashSet<>();
        if (version.getMedia() == null) {
            return allowed;
        }
        for (final Media media : version.getMedia()) {
            if (media.getImage() != null && media.getImage().getId() != null) {
                allowed.add(media.getImage().getId());
            }
        }
        return allowed;
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
