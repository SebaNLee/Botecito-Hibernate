package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.AvailabilityWindow;
import ar.edu.itba.paw.models.dto.ImageUpload;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.mail.MailRecipientModel;
import ar.edu.itba.paw.models.mail.PublishConfirmationMailModel;
import ar.edu.itba.paw.persistence.PublishDao;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Optional<Version> create(
            final int ownerId,
            final int typeId,
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final List<AvailabilityWindow> filteredAvailabilities = filterAvailabilities(availabilities);
        final List<ImageUpload> filteredImages = filterImages(images);

        final int itemId = publishDao.create(
                ownerId,
                typeId,
                title,
                description,
                pricePerHour,
                capacityPeople,
                Objects.requireNonNull(maxWeightKg),
                Objects.requireNonNull(difficultyLevel),
                locationOptionId,
                DEFAULT_TIMEZONE,
                DEFAULT_STATUS,
                filteredAvailabilities,
                filteredImages);

        final Optional<Version> created = publishDao.findById(itemId);
        if (created.isPresent()) {
            sendConfirmationEmail(created.get());
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Version> findById(final int itemId) {
        return publishDao.findById(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Availability> listAvailabilities(final int itemId) {
        return publishDao.listAvailabilities(itemId);
    }

    @Override
    public Map<String, String> validate(
            final String title,
            final String description,
            final int pricePerHour,
            final int capacityPeople,
            final BigDecimal maxWeightKg,
            final Integer difficultyLevel,
            final int locationOptionId,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final Map<String, String> errors = new LinkedHashMap<>();
        if (availabilities == null || availabilities.isEmpty()) {
            errors.put("availabilityByWeekday", "publish.availability.required");
            return errors;
        }
        final Map<DayOfWeek, List<AvailabilityWindow>> grouped = new LinkedHashMap<>();
        for (final AvailabilityWindow slot : availabilities) {
            if (slot == null || slot.getWeekday() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
                errors.put("availabilityByWeekday", "publish.availability.format.invalid");
                return errors;
            }
            grouped.computeIfAbsent(slot.getWeekday(), ignored -> new ArrayList<>())
                    .add(slot);
        }
        for (final Map.Entry<DayOfWeek, List<AvailabilityWindow>> entry : grouped.entrySet()) {
            final List<AvailabilityWindow> sorted = new ArrayList<>(entry.getValue());
            sorted.sort(Comparator.comparing(AvailabilityWindow::getStartTime));
            LocalTime previousEnd = null;
            for (final AvailabilityWindow slot : sorted) {
                if (!slot.getEndTime().isAfter(slot.getStartTime())) {
                    errors.put("availabilityByWeekday", "publish.availability.end.invalid");
                    return errors;
                }
                if (Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes() < 120) {
                    errors.put("availabilityByWeekday", "publish.availability.min.duration");
                    return errors;
                }
                if (previousEnd != null) {
                    if (slot.getStartTime().isBefore(previousEnd)) {
                        errors.put("availabilityByWeekday", "publish.availability.overlap");
                        return errors;
                    }
                    if (Duration.between(previousEnd, slot.getStartTime()).toMinutes() < 30) {
                        errors.put("availabilityByWeekday", "publish.availability.min.separation");
                        return errors;
                    }
                }
                previousEnd = slot.getEndTime();
            }
        }
        return errors;
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
