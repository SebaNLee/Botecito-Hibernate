package ar.edu.itba.paw.persistence;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class PublishJpaDao implements PublishDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
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
            final String timezone,
            final String status,
            final List<AvailabilityWindow> availabilities,
            final List<ImageUpload> images) {
        final LocalDateTime now = LocalDateTime.now();

        final Item item = Item.builder()
                .host(entityManager.getReference(Users.class, ownerId))
                .status(ItemStatusEnum.valueOf(status))
                .createdAt(now)
                .build();
        entityManager.persist(item);

        final Version version = Version.builder()
                .item(item)
                .type(entityManager.getReference(ItemType.class, typeId))
                .title(title)
                .description(description)
                .price(BigDecimal.valueOf(pricePerHour))
                .capacity(capacityPeople)
                .weight(weight)
                .difficulty(difficulty)
                .location(entityManager.getReference(Location.class, locationOptionId))
                .timezone(timezone)
                .createdAt(now)
                .build();
        entityManager.persist(version);
        entityManager.flush();

        for (final AvailabilityWindow window : availabilities) {
            final Availability a = new Availability();
            a.setVersion(version);
            a.setWeekday(WeekdayEnum.valueOf(window.getWeekday().name()));
            a.setStartTime(window.getStartTime());
            a.setEndTime(window.getEndTime());
            entityManager.persist(a);
        }

        for (int idx = 0; idx < images.size(); idx++) {
            final ImageUpload upload = images.get(idx);
            final Image image = new Image();
            image.setData(upload.getData());
            entityManager.persist(image);

            final Media media = new Media();
            media.setId(new MediaId(version.getId(), idx));
            media.setVersion(version);
            media.setImage(image);
            entityManager.persist(media);
        }

        return version;
    }
}
