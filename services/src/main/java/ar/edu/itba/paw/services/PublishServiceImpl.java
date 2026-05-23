package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
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
import ar.edu.itba.paw.models.mail.MailRecipientModel;
import ar.edu.itba.paw.models.mail.PublishConfirmationMailModel;
import ar.edu.itba.paw.persistence.PublishDao;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublishServiceImpl implements PublishService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);

    private static final String DEFAULT_STATUS = "ACTIVE";
    private static final String DEFAULT_TIMEZONE = "America/Argentina/Buenos_Aires"; // TODO hardcode default timezone

    private final PublishDao publishDao;
    private final UserService userService;
    private final MailService mailService;

    @Override
    @Transactional
    public void create(
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

        final Item item = publishDao.persistItem(buildItem(ownerId, now));
        final Version version = publishDao.persistVersion(buildVersion(
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
        publishDao.flush();

        for (final AvailabilityWindow window : filteredAvailabilities) {
            publishDao.persistAvailability(buildAvailability(version, window));
        }

        for (int idx = 0; idx < filteredImages.size(); idx++) {
            final Image image = publishDao.persistImage(buildImage(filteredImages.get(idx)));
            publishDao.persistMedia(buildMedia(version, image, idx));
        }

        sendConfirmationEmail(version);
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

    private void sendConfirmationEmail(final Version version) {
        final int ownerId = version.getItem().getHost().getId();
        final Optional<Users> owner = userService.findById(ownerId);
        if (owner.isEmpty()) {
            LOGGER.warn("Cannot send confirmation email: user {} not found", ownerId);
            return;
        }
        final PublishConfirmationMailModel mail = new PublishConfirmationMailModel();
        mail.setOwner(MailRecipientModel.fromUser(owner.get()));
        mail.setItemTitle(version.getTitle());
        try {
            mailService.sendPublishConfirmationEmail(mail);
        } catch (final RuntimeException e) {
            LOGGER.error("Failed to send confirmation email for item {} to user {}", version.getTitle(), ownerId, e);
        }
    }

    private static List<AvailabilityWindow> filterAvailabilities(final List<AvailabilityWindow> windows) {
        if (windows == null) {
            return List.of();
        }
        return windows.stream()
                .filter(w -> w != null && w.getWeekday() != null && w.getStartTime() != null && w.getEndTime() != null)
                .toList();
    }

    private static List<ImageUpload> filterImages(final List<ImageUpload> imgs) {
        if (imgs == null) {
            return List.of();
        }
        return imgs.stream()
                .filter(u -> u != null && u.getData() != null && u.getData().length > 0)
                .toList();
    }
}
