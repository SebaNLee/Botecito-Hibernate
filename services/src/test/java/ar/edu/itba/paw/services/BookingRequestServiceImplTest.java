package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BookingRequestServiceImplTest {

    @InjectMocks
    private BookingRequestServiceImpl bookingRequestService;

    @Mock
    private ItemDao itemDao;

    @Mock
    private MailService mailService;

    @Test
    public void testCreateBookingRequestWhenUserExists() {
        final User existingUser = new User();
        existingUser.setId(7);
        existingUser.setGivenName("A");
        existingUser.setLastName("A");
        existingUser.setEmail("a@a.com");
        existingUser.setPreferredLanguage("es");

        final ItemBooking createdBooking = new ItemBooking();
        createdBooking.setItemId(15);
        createdBooking.setGuestId(7);
        createdBooking.setHostDecisionToken("t");
        createdBooking.setRequestMessage("a");
        createdBooking.setState(BookingState.BOOKING_PENDING);
        createdBooking.setCreatedAt(OffsetDateTime.now());

        final OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        final OffsetDateTime end = start.plusHours(2);
        Mockito.when(itemDao.findUserByEmail("a@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(itemDao.createBookingRequest(
                        Mockito.eq(15),
                        Mockito.eq(7),
                        Mockito.eq(start),
                        Mockito.eq(end),
                        Mockito.eq("a"),
                        Mockito.anyString()))
                .thenReturn(createdBooking);
        final BookingRequest result =
                bookingRequestService.createBookingRequest(15, " A ", " B ", "a@a.com", "en", start, end, "a");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("t", result.getToken());
        Assertions.assertEquals("A B", result.getRequesterName());
        Assertions.assertEquals("en", result.getRequesterLocaleTag());
        Mockito.verify(itemDao).updateUserProfile(7, "A", "B", "en");
        Mockito.verify(itemDao, Mockito.never())
                .createUser(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testFindByTokenWhenRequesterDoesNotExist() {
        final ItemBooking booking = new ItemBooking();
        booking.setGuestId(99);
        booking.setHostDecisionToken("t");

        Mockito.when(itemDao.findBookingByHostDecisionToken("t")).thenReturn(Optional.of(booking));
        Mockito.when(itemDao.findUserById(99)).thenReturn(Optional.empty());
        final Optional<BookingRequest> result = bookingRequestService.findByToken("t");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testResolveBookingRequestWhenTokenCannotBeResolved() {
        Mockito.when(itemDao.resolveBookingByHostDecisionToken(
                        Mockito.eq("t"), Mockito.eq(BookingState.BOOKING_CONFIRMED), Mockito.any()))
                .thenReturn(false);
        final Optional<BookingRequest> result =
                bookingRequestService.resolveBookingRequest("t", BookingState.BOOKING_CONFIRMED);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testResolveBookingRequestWhenTokenIsResolved() {
        final ItemBooking booking = new ItemBooking();
        booking.setItemId(20);
        booking.setGuestId(5);
        booking.setHostDecisionToken("t");
        booking.setRequestMessage("a");
        booking.setState(BookingState.BOOKING_CONFIRMED);
        booking.setCreatedAt(OffsetDateTime.now());
        booking.setHostDecisionUsedAt(OffsetDateTime.now());

        final User user = new User();
        user.setId(5);
        user.setGivenName("A");
        user.setLastName("A");
        user.setEmail("a@a.com");
        user.setPreferredLanguage("es");

        Mockito.when(itemDao.resolveBookingByHostDecisionToken(
                        Mockito.eq("t"), Mockito.eq(BookingState.BOOKING_CONFIRMED), Mockito.any()))
                .thenReturn(true);
        Mockito.when(itemDao.findBookingByHostDecisionToken("t")).thenReturn(Optional.of(booking));
        Mockito.when(itemDao.findUserById(5)).thenReturn(Optional.of(user));
        final Optional<BookingRequest> result =
                bookingRequestService.resolveBookingRequest("t", BookingState.BOOKING_CONFIRMED);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("t", result.get().getToken());
        Assertions.assertEquals("a@a.com", result.get().getRequesterEmail());
    }
}
