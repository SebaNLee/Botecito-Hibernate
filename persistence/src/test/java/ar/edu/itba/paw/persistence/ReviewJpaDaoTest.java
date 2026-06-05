package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.*;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
@Transactional
public class ReviewJpaDaoTest {

    @Autowired
    private ReviewDao reviewDao;

    @PersistenceContext
    private EntityManager em;

    private Users host;
    private Users guest;
    private Location location;
    private ItemType itemType;

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        guest = insertUser(em, "Guest", "User", "botecito.user@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        em.flush();
    }

    @Test
    public void testCreateReview() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();

        Optional<Review> created =
                reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.ITEM, 4.5, "Great!");

        assertTrue(created.isPresent());
        assertEquals(4.5, created.get().getRating().doubleValue(), 0.01);
    }

    @Test
    public void testFindReviewByBookingSenderAndTargetType() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.ITEM, 4.5, "Great!");
        em.flush();

        Optional<Review> found =
                reviewDao.findReviewByBookingSenderAndTargetType(booking.getId(), guest.getId(), TargetEnum.ITEM);

        assertTrue(found.isPresent());
    }

    @Test
    public void testFindReviewsBySender() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.ITEM, 4.0, "First");
        reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.USER, 5.0, "Second");
        em.flush();

        List<Review> reviews = reviewDao.findReviewsBySender(guest.getId());

        assertEquals(2, reviews.size());
    }

    @Test
    public void testFindReviewsAboutHost() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.USER, 5.0, "Great host!");
        em.flush();

        List<Review> reviews = reviewDao.findReviewsAboutHost(host.getId(), 1, 12);

        assertEquals(1, reviews.size());
    }
}
