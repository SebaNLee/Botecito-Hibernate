package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.dto.MyBoatsQueryModel;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.MediaId;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.entity.WeekdayEnum;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.InactiveItemException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.ItemDao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemImpl implements ItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemImpl.class);

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires";

    private final ItemDao itemDao;

    @Override
    @Transactional(readOnly = true)
    public PageModel<Item> listOwnerItems(
            int ownerId, String searchQuery, String status, int page, int pageSize, String sortBy) {
        final MyBoatsQueryModel query = MyBoatsQueryModel.builder()
                .ownerId(ownerId)
                .searchQuery(searchQuery)
                .status(status)
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .build();

        return itemDao.listOwnerItems(query);
    }

    @Override
    @Transactional(readOnly = true)
    public Item findItemById(int id) {
        return itemDao.findItemById(id).orElseThrow(ItemNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Image> findImageById(int id) {
        var image = itemDao.findImageById(id);
        image.ifPresent(Image::getData);
        return image;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userOwnsItem(Item item, int userId) {
        return item.getHost().getId().equals(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userOwnsItem(int itemId, int userId) {
        var item = findItemById(itemId);
        return userOwnsItem(item, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Item requireOwnedItem(int itemId, int userId) {
        var item = findItemById(itemId);
        if (!userOwnsItem(item, userId)) throw new ForbiddenOperationException();
        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public Item requireOwnedActiveItem(final int itemId, final int userId) {
        final Item item = requireOwnedItem(itemId, userId);
        if (item.getStatus() != ItemStatusEnum.ACTIVE) {
            throw new InactiveItemException();
        }
        return item;
    }

    @Override
    @Transactional(readOnly = true)
    public Version requireOwnedFullData(int itemId, int userId) {
        var version = requireOwnedActiveItem(itemId, userId).getLatestVersion();
        var avail = version.getAvailabilities();
        var media = version.getMedia();

        // Force hibernate to load the contents of collections
        LOGGER.debug("Force-loading {} availabilities and {} media references.", avail.size(), media.size());

        return version;
    }

    @Override
    @Transactional(readOnly = true)
    public int getVersionCount(int itemId) {
        return itemDao.getVersionCount(itemId);
    }

    @Override
    @Transactional
    public void deleteItem(Item item, boolean soft) {
        if (soft) {
            applySoftDelete(item);
            return;
        }
        itemDao.deleteItem(item);
    }

    @Override
    @Transactional
    public Version create(
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
        final List<AvailabilityWindow> filteredAvailabilities = filterAvailabilities(availabilities);
        final List<ImageUpload> filteredImages = filterImages(images);
        final LocalDateTime now = LocalDateTime.now();

        final Item item = itemDao.persistItem(buildItem(ownerId, now));
        final Version version = itemDao.persistVersion(buildVersion(
                item,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                weight,
                Objects.requireNonNull(difficulty),
                locationOptionId,
                now));
        persistAvailabilities(version, filteredAvailabilities);
        persistMedia(version, filteredImages, Set.of());
        return version;
    }

    @Override
    @Transactional
    public void createNewVersion(
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
        final Version version = itemDao.persistVersion(buildNewVersion(
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
        persistAvailabilities(version, filterAvailabilities(availabilities));
        persistMedia(version, filterImages(images), allowedExistingImageIds(current));
    }

    @Override
    @Transactional
    public void overwriteVersion(
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
        final Version version = itemDao.findVersionById(versionId)
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

        itemDao.removeVersionChildren(version);
        persistAvailabilities(version, filterAvailabilities(availabilities));
        persistMedia(version, filterImages(images), Set.of());
    }

    private void applySoftDelete(final Item item) {
        item.setStatus(ItemStatusEnum.DELETED);
        itemDao.deleteVersion(item.getLatestVersion());
    }

    private void persistAvailabilities(final Version version, final List<AvailabilityWindow> availabilities) {
        for (final AvailabilityWindow window : availabilities) {
            itemDao.persistAvailability(buildAvailability(version, window));
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
                image = itemDao.persistImage(buildImage(upload));
            }
            itemDao.persistMedia(buildMedia(version, image, idx));
        }
    }

    private static Item buildItem(final int ownerId, final LocalDateTime createdAt) {
        return Item.builder()
                .host(userReference(ownerId))
                .status(ItemStatusEnum.valueOf(DEFAULT_STATUS))
                .createdAt(createdAt)
                .build();
    }

    private static Version buildVersion(
            final Item item,
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
                .item(item)
                .type(itemTypeReference(typeId))
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(pricePerHour))
                .capacity(capacityPeople)
                .weight(weight)
                .difficulty(difficulty)
                .location(locationReference(locationOptionId))
                .timezone(DEFAULT_TIMEZONE)
                .createdAt(createdAt)
                .build();
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

    private static Image buildImage(final ImageUpload upload) {
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

    private static Users userReference(final int userId) {
        final Users user = new Users();
        user.setId(userId);
        return user;
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
