package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.*;
import ar.edu.itba.paw.models.exceptions.*;
import ar.edu.itba.paw.persistence.BookingDao;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BookingImplTest {

    private static final int ITEM_ID = 1;
    private static final int OWNER_ID = 10;
    private static final int GUEST_ID = 20;
    private static final int OTHER_USER_ID = 30;
    private static final int BOOKING_ID = 100;
    private static final LocalDate DATE = LocalDate.of(3026, 6, 3);

    @Mock
    private BookingDao bookingDao;

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @InjectMocks
    private BookingImpl bookingService;

    @Test
    public void testCreate() {
        when(userService.findById(GUEST_ID)).thenReturn(Optional.of(user(GUEST_ID)));
        when(itemService.findItemById(ITEM_ID)).thenReturn(activeItem());
        when(bookingDao.canInsertBooking(any(), any())).thenReturn(true);

        assertDoesNotThrow(() ->
                bookingService.createBooking(ITEM_ID, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0), "msg", GUEST_ID));
    }

    @Test
    public void testSearch() {
        var result = new PageModel<Booking>(List.of(), 1, 12, 0);
        when(bookingDao.searchBookings(any())).thenReturn(result);

        var actual = bookingService.searchBookings(OWNER_ID, true, null, null, null, 1, 10, null);

        assertEquals(result, actual);
    }

    @Test
    public void testCreateThrowsOnCollision() {
        when(userService.findById(GUEST_ID)).thenReturn(Optional.of(user(GUEST_ID)));
        when(itemService.findItemById(ITEM_ID)).thenReturn(activeItem());
        when(bookingDao.canInsertBooking(any(), any())).thenReturn(false);

        assertThrows(
                BookingCollisionException.class,
                () -> bookingService.createBooking(
                        ITEM_ID, DATE, LocalTime.of(10, 0), LocalTime.of(12, 0), "msg", GUEST_ID));
    }

    @Test
    public void testAccept() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PENDING)));

        assertDoesNotThrow(() -> bookingService.acceptBooking(BOOKING_ID, OWNER_ID));
    }

    @Test
    public void testDoesNotAccept() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PENDING)));

        assertThrows(
                IllegalBookingOperationException.class, () -> bookingService.acceptBooking(BOOKING_ID, OTHER_USER_ID));
    }

    @Test
    public void testReject() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PENDING)));

        assertDoesNotThrow(() -> bookingService.rejectBooking(BOOKING_ID, OWNER_ID));
    }

    @Test
    public void testRejectThrowsWhenNotOwner() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PENDING)));

        assertThrows(
                IllegalBookingOperationException.class, () -> bookingService.rejectBooking(BOOKING_ID, OTHER_USER_ID));
    }

    @Test
    public void testSubmitPayment() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.ACCEPTED)));

        assertDoesNotThrow(() -> bookingService.submitPayment(
                BOOKING_ID, "file.pdf", "application/pdf", new byte[] {1}, null, GUEST_ID));
    }

    @Test
    public void testSubmitPaymentThrowsWhenNotGuest() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.ACCEPTED)));

        assertThrows(
                IllegalBookingOperationException.class,
                () -> bookingService.submitPayment(
                        BOOKING_ID, "file.pdf", "application/pdf", new byte[] {1}, null, OTHER_USER_ID));
    }

    @Test
    public void testPaymentProofAsHost() {
        var proof = PaymentProof.builder().id(1).build();
        var b = booking(BookingStatusEnum.PENDING);
        b.setPaymentProof(proof);
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(b));

        var result = bookingService.getPaymentProofForParticipant(BOOKING_ID, OWNER_ID);

        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get().getId());
    }

    @Test
    public void testConfirmPayment() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PAID)));

        assertDoesNotThrow(() -> bookingService.confirmPayment(BOOKING_ID, OWNER_ID));
    }

    @Test
    public void testConfirmPaymentThrowsWhenNotOwner() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PAID)));

        assertThrows(
                IllegalBookingOperationException.class, () -> bookingService.confirmPayment(BOOKING_ID, OTHER_USER_ID));
    }

    @Test
    public void testRejectPayment() {
        var b = booking(BookingStatusEnum.PAID);
        b.setPaymentProof(new PaymentProof());
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(b));

        assertDoesNotThrow(() -> bookingService.rejectPayment(BOOKING_ID, OWNER_ID, "bad proof"));
    }

    @Test
    public void testRejectPaymentThrowsWhenNotOwner() {
        var b = booking(BookingStatusEnum.PAID);
        b.setPaymentProof(new PaymentProof());
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(b));

        assertThrows(
                IllegalBookingOperationException.class,
                () -> bookingService.rejectPayment(BOOKING_ID, OTHER_USER_ID, "bad proof"));
    }

    @Test
    public void testCancel() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PENDING)));

        assertDoesNotThrow(() -> bookingService.cancelBooking(BOOKING_ID, GUEST_ID));
    }

    @Test
    public void testCancelThrowsWhenNotGuest() {
        when(bookingDao.findById(BOOKING_ID)).thenReturn(Optional.of(booking(BookingStatusEnum.PENDING)));

        assertThrows(
                IllegalBookingOperationException.class, () -> bookingService.cancelBooking(BOOKING_ID, OTHER_USER_ID));
    }

    private static Item activeItem() {
        var host = user(OWNER_ID);
        var item = Item.builder()
                .id(ITEM_ID)
                .host(host)
                .status(ItemStatusEnum.ACTIVE)
                .build();
        var availability = new Availability();
        availability.setWeekday(WeekdayEnum.SATURDAY);
        availability.setStartTime(LocalTime.of(8, 0));
        availability.setEndTime(LocalTime.of(20, 0));
        var version = Version.builder()
                .item(item)
                .timezone("UTC")
                .availabilities(List.of(availability))
                .build();
        item.setLatestVersion(version);
        return item;
    }

    private static Booking booking(BookingStatusEnum status) {
        return Booking.builder()
                .version(activeItem().getLatestVersion())
                .guest(user(GUEST_ID))
                .start(LocalDateTime.of(3026, 6, 3, 10, 0))
                .end(LocalDateTime.of(3026, 6, 3, 12, 0))
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
