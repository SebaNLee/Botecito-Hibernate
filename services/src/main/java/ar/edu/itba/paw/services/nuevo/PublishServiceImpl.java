package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ImageUpload;
import ar.edu.itba.paw.models.nuevo.PublishContent;
import ar.edu.itba.paw.models.nuevo.PublishItem;
import ar.edu.itba.paw.models.nuevo.UserModel;
import ar.edu.itba.paw.models.nuevo.mail.MailRecipientModel;
import ar.edu.itba.paw.models.nuevo.mail.PublishConfirmationMailModel;
import ar.edu.itba.paw.persistence.nuevo.PublishDao;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private final PublishDao publishDao;
    private final UserService userService;
    private final MailService mailService;

    @Override
    @Transactional
    public Optional<PublishItem> create(final PublishContent draft, final int ownerId) {
        final int itemId = publishDao.create(
                ownerId,
                draft.getTypeId(),
                draft.getTitle(),
                draft.getDescription(),
                draft.getPricePerHour(),
                draft.getCapacityPeople(),
                draft.getMaxWeightKg(),
                draft.getDifficultyLevel(),
                draft.getLocationOptionId(),
                draft.getAvailabilities(),
                mapToImageUploads(draft));
        if (itemId <= 0) {
            LOGGER.error("Failed to create publication for owner {}", ownerId);
            return Optional.empty();
        }

        final Optional<PublishItem> created = publishDao.findById(itemId);
        if (created.isPresent()) {
            sendConfirmationEmail(ownerId, created.get().getTitle());
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublishItem> findById(final int itemId) {
        return publishDao.findById(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityWindow> listAvailabilities(final int itemId) {
        return publishDao.listAvailabilities(itemId);
    }

    @Override
    public Map<String, String> validate(final PublishContent draft) {
        final Map<String, String> errors = new LinkedHashMap<>();
        if (draft == null) {
            errors.put("draft", "publish.validation.required");
            return errors;
        }
        final List<AvailabilityWindow> availabilities = draft.getAvailabilities();
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

    private void sendConfirmationEmail(final int ownerId, final String itemTitle) {
        final Optional<UserModel> owner = userService.findById(ownerId);
        if (owner.isEmpty()) {
            LOGGER.warn("Cannot send confirmation email: user {} not found", ownerId);
            return;
        }
        final PublishConfirmationMailModel mail = new PublishConfirmationMailModel();
        mail.setOwner(MailRecipientModel.fromUser(owner.get()));
        mail.setItemTitle(itemTitle);
        try {
            mailService.sendPublishConfirmationEmail(mail);
        } catch (final RuntimeException e) {
            LOGGER.error("Failed to send confirmation email for item {} to user {}", itemTitle, ownerId, e);
        }
    }

    private static List<ImageUpload> mapToImageUploads(final PublishContent draft) {
        final List<ImageUpload> images = draft.getImages();
        return images == null ? List.of() : images;
    }
}
