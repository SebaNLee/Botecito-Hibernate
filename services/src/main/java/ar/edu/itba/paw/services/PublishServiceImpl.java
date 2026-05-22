package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.mail.MailRecipientModel;
import ar.edu.itba.paw.models.mail.PublishConfirmationMailModel;
import ar.edu.itba.paw.persistence.PublishDao;
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

        final Version created = publishDao.create(
                ownerId,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                weight,
                Objects.requireNonNull(difficulty),
                locationOptionId,
                DEFAULT_TIMEZONE,
                DEFAULT_STATUS,
                filteredAvailabilities,
                filteredImages);

        sendConfirmationEmail(created);
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
