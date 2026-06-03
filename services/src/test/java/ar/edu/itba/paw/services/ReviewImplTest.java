package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.*;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ReviewDao;
import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private BookingDao bookingDao;

    @Mock
    private ReviewDao reviewDao;

    @InjectMocks
    private ReviewImpl reviewService;

    @Test
    public void testCreate() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(finishedBooking()));
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
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.empty());

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "Great!");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateBookingNotFinished() {
        var booking = bookingWithStatus(BookingStatusEnum.PENDING);
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "Great!");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testCreateDuplicate() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(finishedBooking()));
        when(reviewDao.findReviewByBookingSenderAndTargetType(BOOKING_ID, GUEST_ID, TargetEnum.ITEM))
                .thenReturn(Optional.of(new Review()));

        var result = reviewService.createReviewForBooking(BOOKING_ID, GUEST_ID, 5, "Great!");

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByBookingIds() {
        var review = new Review();
        review.setBooking(finishedBooking());
        when(reviewDao.findReviewsBySender(GUEST_ID)).thenReturn(List.of(review));

        var result = reviewService.findReviewsByBookingIds(GUEST_ID);

        assertEquals(1, result.size());
    }

    private static Booking finishedBooking() {
        var host = new Users();
        host.setId(OWNER_ID);
        var item = new Item();
        item.setHost(host);
        var version = new Version();
        version.setItem(item);
        var guest = new Users();
        guest.setId(GUEST_ID);
        var booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setVersion(version);
        booking.setGuest(guest);
        booking.setStatus(BookingStatusEnum.FINISHED);
        booking.setEnd(LocalDateTime.of(2020, 1, 1, 12, 0));
        return booking;
    }

    private static Booking bookingWithStatus(BookingStatusEnum status) {
        var host = new Users();
        host.setId(OWNER_ID);
        var item = new Item();
        item.setHost(host);
        var version = new Version();
        version.setItem(item);
        var guest = new Users();
        guest.setId(GUEST_ID);
        var booking = new Booking();
        booking.setId(BOOKING_ID);
        booking.setVersion(version);
        booking.setGuest(guest);
        booking.setStatus(status);
        booking.setEnd(LocalDateTime.of(2020, 1, 1, 12, 0));
        return booking;
    }
}
