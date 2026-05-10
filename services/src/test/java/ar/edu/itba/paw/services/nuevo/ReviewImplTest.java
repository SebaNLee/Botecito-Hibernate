package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.nuevo.PendingReviewActionModel;
import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.ReviewTargetType;
import ar.edu.itba.paw.persistence.ItemBookingDao;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.persistence.UserDao;
import ar.edu.itba.paw.persistence.nuevo.ReviewDao;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReviewImplTest {

    private static final int BOOKING_ID = 100;
    private static final int ITEM_ID = 200;
    private static final int OWNER_ID = 300;
    private static final int GUEST_ID = 400;

    @InjectMocks
    private ReviewImpl reviewService;

    @Mock
    private ItemDao itemDao;

    @Mock
    private ItemBookingDao itemBookingDao;

    @Mock
    private ReviewDao reviewDao;

    @Mock
    private UserDao userDao;

    @Test
    public void testCreateReviewForBookingRejectsRatingOutOfRange() {
        Assertions.assertTrue(reviewService
                .createReviewForBooking(BOOKING_ID, GUEST_ID, 0, "x")
                .isEmpty());
        Assertions.assertTrue(reviewService
                .createReviewForBooking(BOOKING_ID, GUEST_ID, 6, "x")
                .isEmpty());
        Mockito.verifyNoInteractions(reviewDao);
    }

    @Test
    public void testCreateReviewForBookingPersistsItemReviewWhenGuestReviews() {
        final ItemBooking booking = pastCompletedBooking();
        final Item item = item();
        Mockito.when(itemBookingDao.findBookingById(BOOKING_ID)).thenReturn(Optional.of(booking));
        Mockito.when(itemDao.findAnyItemById(ITEM_ID)).thenReturn(Optional.of(item));
        Mockito.when(reviewDao.createReview(
                        Mockito.eq(BOOKING_ID),
                        Mockito.eq(GUEST_ID),
                        Mockito.eq(OWNER_ID),
                        Mockito.eq(ReviewTargetType.ITEM),
                        Mockito.eq(ITEM_ID),
                        Mockito.eq(5),
                        ArgumentMatchers.nullable(String.class)))
                .thenReturn(Optional.of(new ReviewModel()));

        final Optional<ReviewModel> created =
                reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "  great  ");

        Assertions.assertTrue(created.isPresent());
        Mockito.verify(reviewDao)
                .createReview(BOOKING_ID, GUEST_ID, OWNER_ID, ReviewTargetType.ITEM, ITEM_ID, 5, "great");
    }

    @Test
    public void testCreateReviewForBookingFailsWhenBookingHasNotEnded() {
        final ItemBooking booking = pastCompletedBooking();
        booking.setEndTime(OffsetDateTime.now().plusDays(1));
        Mockito.when(itemBookingDao.findBookingById(BOOKING_ID)).thenReturn(Optional.of(booking));

        Assertions.assertTrue(reviewService
                .createReviewForBooking(BOOKING_ID, GUEST_ID, 4, null)
                .isEmpty());
        Mockito.verifyNoInteractions(reviewDao);
    }

    @Test
    public void testGetItemRatingSummaryDelegatesToDao() {
        final RatingSummaryModel summary = new RatingSummaryModel(4.2, 7);
        Mockito.when(reviewDao.ratingSummaryByTarget(ReviewTargetType.ITEM, ITEM_ID))
                .thenReturn(summary);

        Assertions.assertSame(summary, reviewService.getItemRatingSummary(ITEM_ID));
    }

    @Test
    public void testFindPendingItemReviewActionReturnsActionForEligibleGuest() {
        final ItemBooking booking = pastCompletedBooking();
        Mockito.when(itemBookingDao.listBookingsByGuestId(GUEST_ID)).thenReturn(List.of(booking));
        Mockito.when(itemDao.findAnyItemById(ITEM_ID)).thenReturn(Optional.of(item()));
        Mockito.when(reviewDao.findReviewByBookingReviewerAndTargetType(BOOKING_ID, GUEST_ID, ReviewTargetType.ITEM))
                .thenReturn(Optional.empty());
        final User owner = new User();
        owner.setId(OWNER_ID);
        owner.setEmail("o@example.com");
        owner.setGivenName("Owner");
        Mockito.when(userDao.findById(OWNER_ID)).thenReturn(Optional.of(owner));

        final Optional<PendingReviewActionModel> action = reviewService.findPendingItemReviewAction(GUEST_ID, ITEM_ID);

        Assertions.assertTrue(action.isPresent());
        Assertions.assertEquals(BOOKING_ID, action.get().getBookingId());
        Assertions.assertEquals(ReviewTargetType.ITEM, action.get().getTargetType());
        Assertions.assertEquals(OWNER_ID, action.get().getTargetUserId());
    }

    @Test
    public void testFindPendingItemReviewActionEmptyWhenAlreadyReviewed() {
        final ItemBooking booking = pastCompletedBooking();
        Mockito.when(itemBookingDao.listBookingsByGuestId(GUEST_ID)).thenReturn(List.of(booking));
        Mockito.when(itemDao.findAnyItemById(ITEM_ID)).thenReturn(Optional.of(item()));
        Mockito.when(reviewDao.findReviewByBookingReviewerAndTargetType(BOOKING_ID, GUEST_ID, ReviewTargetType.ITEM))
                .thenReturn(Optional.of(new ReviewModel()));

        Assertions.assertTrue(
                reviewService.findPendingItemReviewAction(GUEST_ID, ITEM_ID).isEmpty());
    }

    @Test
    public void testDeleteReviewDelegatesToDao() {
        Mockito.when(reviewDao.deleteReview(11, GUEST_ID)).thenReturn(true);
        Assertions.assertTrue(reviewService.deleteReview(11, GUEST_ID));
    }

    private static ItemBooking pastCompletedBooking() {
        final ItemBooking booking = new ItemBooking();
        booking.setId(BOOKING_ID);
        booking.setItemId(ITEM_ID);
        booking.setGuestId(GUEST_ID);
        booking.setStartTime(OffsetDateTime.now().minusDays(2));
        booking.setEndTime(OffsetDateTime.now().minusDays(1));
        booking.setState(BookingState.BOOKING_COMPLETED);
        return booking;
    }

    private static Item item() {
        final Item item = new Item();
        item.setId(ITEM_ID);
        item.setOwnerId(OWNER_ID);
        return item;
    }
}
