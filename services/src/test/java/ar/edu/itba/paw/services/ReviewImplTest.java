package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.HostReviewStats;
import ar.edu.itba.paw.models.dto.ReviewSummary;
import ar.edu.itba.paw.models.entity.*;
import ar.edu.itba.paw.persistence.ReviewDao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReviewImplTest {

    private static final int BOOKING_ID = 1;
    private static final int GUEST_ID = 10;
    private static final int OWNER_ID = 20;
    private static final int ITEM_ID = 5;

    @Mock
    private BookingService bookingService;

    @Mock
    private ReviewDao reviewDao;

    @InjectMocks
    private ReviewImpl reviewService;

    @Test
    public void testCreate() {
        when(bookingService.findById(BOOKING_ID)).thenReturn(finishedBooking());
        when(reviewDao.findReviewByBookingSenderAndTargetType(BOOKING_ID, GUEST_ID, TargetEnum.ITEM))
                .thenReturn(Optional.empty());
        when(reviewDao.createReview(anyInt(), anyInt(), any(), anyDouble(), any()))
                .thenReturn(Optional.of(new Review()));

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "Great!");

        assertTrue(result.isPresent());
    }

    @Test
    public void testCreateRatingInvalid() {
        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 0, "Great!");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateBookingNotFound() {
        when(bookingService.findById(BOOKING_ID)).thenReturn(null);

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "Great!");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateBookingNotFinished() {
        var booking = bookingWithStatus(BookingStatusEnum.PENDING);
        when(bookingService.findById(BOOKING_ID)).thenReturn(booking);

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "Great!");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByBookingIds() {
        var review = new Review();
        review.setBooking(finishedBooking());
        when(reviewDao.findReviewsBySenderAndBookingIds(eq(GUEST_ID), any())).thenReturn(List.of(review));

        var result = reviewService.findReviewsByBookingIds(GUEST_ID, List.of(BOOKING_ID));

        assertEquals(1, result.size());
    }

    @Test
    public void testFindByBookingIdsEmptyInputReturnsEmptyMap() {
        var result = reviewService.findReviewsByBookingIds(GUEST_ID, List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateReviewHostCannotReviewGuest() {
        when(bookingService.findById(BOOKING_ID)).thenReturn(finishedBooking());

        var result = reviewService.createReviewForBooking(BOOKING_ID, OWNER_ID, 4, "Good guest", TargetEnum.USER);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateReviewGuestCanReviewHost() {
        when(bookingService.findById(BOOKING_ID)).thenReturn(finishedBooking());
        when(reviewDao.findReviewByBookingSenderAndTargetType(BOOKING_ID, GUEST_ID, TargetEnum.USER))
                .thenReturn(Optional.empty());
        when(reviewDao.createReview(anyInt(), anyInt(), any(), anyDouble(), any()))
                .thenReturn(Optional.of(new Review()));

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 4, "Great host", TargetEnum.USER);

        assertTrue(result.isPresent());
    }

    @Test
    public void testFindHostReviewsPage() {
        var stats = new HostReviewStats(5L, Optional.of(4.5));
        when(reviewDao.hostReviewStats(OWNER_ID)).thenReturn(stats);
        when(reviewDao.findReviewsAboutHost(anyInt(), anyInt(), anyInt())).thenReturn(List.of(new Review()));

        var result = reviewService.findHostReviewsPage(OWNER_ID, 1);

        assertNotNull(result);
        assertEquals(5L, result.getReviews().getTotalItems());
    }

    @Test
    public void testAttachReviewSummaries() {
        var items = List.of(item(1), item(2));
        var summaries = Map.of(1, new ReviewSummary(3L, 4.5), 2, ReviewSummary.EMPTY);
        when(reviewDao.reviewSummariesForItems(any())).thenReturn(summaries);

        reviewService.attachReviewSummaries(items);

        assertEquals(3L, items.get(0).getReviewSummary().getTotalReviews());
        assertEquals(0L, items.get(1).getReviewSummary().getTotalReviews());
    }

    @Test
    public void testAttachItemReviews() {
        var item = item(ITEM_ID);
        var summary = new ReviewSummary(2L, 3.5);
        when(reviewDao.reviewSummaryForItem(ITEM_ID)).thenReturn(summary);
        when(reviewDao.findReviewsAboutItem(anyInt(), anyInt(), anyInt())).thenReturn(List.of(new Review()));

        reviewService.attachItemReviews(item, ITEM_ID, 1);

        assertNotNull(item.getReviewSummary());
        assertEquals(2L, item.getReviewSummary().getTotalReviews());
    }

    private static Booking finishedBooking() {
        var host = user(OWNER_ID);
        var item = new Item();
        item.setHost(host);
        var version = new Version();
        version.setItem(item);
        var guest = user(GUEST_ID);
        var booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setVersion(version);
        booking.setGuest(guest);
        booking.setStatus(BookingStatusEnum.FINISHED);
        booking.setEnd(LocalDateTime.of(2020, 1, 1, 12, 0));
        return booking;
    }

    private static Booking bookingWithStatus(BookingStatusEnum status) {
        var host = user(OWNER_ID);
        var item = new Item();
        item.setHost(host);
        var version = new Version();
        version.setItem(item);
        var guest = user(GUEST_ID);
        var booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setVersion(version);
        booking.setGuest(guest);
        booking.setStatus(status);
        booking.setEnd(LocalDateTime.of(2020, 1, 1, 12, 0));
        return booking;
    }
}
