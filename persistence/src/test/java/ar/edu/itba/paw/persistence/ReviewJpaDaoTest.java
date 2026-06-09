package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.HostReviewStats;
import ar.edu.itba.paw.models.dto.ReviewSummary;
import ar.edu.itba.paw.models.entity.*;
import java.util.List;
import java.util.Map;
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

    @Test
    public void testHostReviewStats() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.USER, 4.5, "Great host!");
        em.flush();

        HostReviewStats stats = reviewDao.hostReviewStats(host.getId());

        assertEquals(1, stats.getTotalReviews());
        assertTrue(stats.getAverageRating().isPresent());
        assertEquals(4.5, stats.getAverageRating().get(), 0.01);
    }

    @Test
    public void testReviewSummaryForItem() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking.getId(), guest.getId(), TargetEnum.ITEM, 4.0, "Good!");
        em.flush();

        ReviewSummary summary = reviewDao.reviewSummaryForItem(item.getId());

        assertEquals(1, summary.getTotalReviews());
        assertEquals(4.0, summary.getAverageRating(), 0.01);
    }

    @Test
    public void testReviewSummariesForItems() {
        Item item1 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Item item2 = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version1 = insertVersion(em, item1, itemType, location, "Boat 1");
        Version version2 = insertVersion(em, item2, itemType, location, "Boat 2");
        Booking booking1 = insertBooking(em, version1, guest, BookingStatusEnum.FINISHED);
        Booking booking2 = insertBooking(em, version2, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking1.getId(), guest.getId(), TargetEnum.ITEM, 4.0, "Good");
        reviewDao.createReview(booking2.getId(), guest.getId(), TargetEnum.ITEM, 5.0, "Great");
        em.flush();

        Map<Integer, ReviewSummary> result = reviewDao.reviewSummariesForItems(List.of(item1.getId(), item2.getId()));

        assertEquals(2, result.size());
        assertEquals(4.0, result.get(item1.getId()).getAverageRating(), 0.01);
        assertEquals(5.0, result.get(item2.getId()).getAverageRating(), 0.01);
    }

    @Test
    public void testFindReviewsAboutItem() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        Version version = insertVersion(em, item, itemType, location, "Boat");
        Booking booking1 = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking1.getId(), guest.getId(), TargetEnum.ITEM, 4.0, "Good");
        Booking booking2 = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        em.flush();
        reviewDao.createReview(booking2.getId(), guest.getId(), TargetEnum.ITEM, 5.0, "Great");
        em.flush();

        List<Review> reviews = reviewDao.findReviewsAboutItem(item.getId(), 1, 12);

        assertEquals(2, reviews.size());
    }
}
