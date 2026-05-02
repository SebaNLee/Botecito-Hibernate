package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.OffsetDateTime;
import java.util.List;
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
        createdBooking.setHostDecisionToken("ignored");
        createdBooking.setRequestMessage("a");
        createdBooking.setState(BookingState.BOOKING_PENDING);
        createdBooking.setCreatedAt(OffsetDateTime.now());

        final OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        final OffsetDateTime end = start.plusHours(2);
        final Item item = new Item();
        item.setId(15);
        item.setOwnerId(99);
        item.setTitle("Item A");
        item.setLocation("Dock A");
        final User owner = new User();
        owner.setId(99);
        owner.setEmail("owner@a.com");
        Mockito.when(itemDao.findAnyItemById(15)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.findUserById(99)).thenReturn(Optional.of(owner));
        Mockito.when(itemDao.findUserByEmail("a@a.com")).thenReturn(Optional.of(existingUser));
        Mockito.when(itemDao.createBookingRequest(
                        Mockito.eq(15),
                        Mockito.eq(7),
                        Mockito.eq(start),
                        Mockito.eq(end),
                        Mockito.eq("a"),
                        Mockito.anyString()))
                .thenAnswer(invocation -> {
                    createdBooking.setHostDecisionToken(invocation.getArgument(5));
                    return createdBooking;
                });
        final BookingRequest result =
                bookingRequestService.createBookingRequest(15, " A ", " B ", "a@a.com", "en", start, end, "a");
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getToken());
        Assertions.assertFalse(result.getToken().isBlank());
        Assertions.assertEquals("A B", result.getRequesterName());
        Assertions.assertEquals("en", result.getRequesterLocaleTag());
        Mockito.verify(mailService)
                .sendBookingReviewEmail(
                        Mockito.same(result),
                        Mockito.eq("owner@a.com"),
                        Mockito.eq("Item A"),
                        Mockito.eq("Dock A"),
                        Mockito.eq(start.toLocalDate().toString()),
                        Mockito.eq(start.toLocalTime().withSecond(0).withNano(0) + " - "
                                + end.toLocalTime().withSecond(0).withNano(0)));
    }

    @Test
    public void testFindByTokenReturnsEmptyWhenRequesterDoesNotExist() {
        final ItemBooking booking = new ItemBooking();
        booking.setGuestId(99);
        booking.setHostDecisionToken("t");

        Mockito.when(itemDao.findBookingByHostDecisionToken("t")).thenReturn(Optional.of(booking));
        Mockito.when(itemDao.findUserById(99)).thenReturn(Optional.empty());

        final Optional<BookingRequest> result = bookingRequestService.findByToken("t");

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(mailService, Mockito.never()).sendBookingResolutionEmail(Mockito.any());
    }

    @Test
    public void testResolveBookingRequestReturnsEmptyWhenTokenCannotBeResolved() {
        Mockito.when(itemDao.resolveBookingByHostDecisionToken(
                        Mockito.eq("t"), Mockito.eq(BookingState.BOOKING_REJECTED), Mockito.any()))
                .thenReturn(false);

        final Optional<BookingRequest> result =
                bookingRequestService.resolveBookingRequest("t", BookingState.BOOKING_REJECTED);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testResolveBookingRequestReturnsResolvedRequestWhenTokenIsValid() {
        final ItemBooking booking = new ItemBooking();
        booking.setItemId(20);
        booking.setGuestId(5);
        booking.setHostDecisionToken("t");
        booking.setRequestMessage("a");
        booking.setState(BookingState.BOOKING_CONFIRMED);
        booking.setStartTime(OffsetDateTime.now().plusDays(1));
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
        Assertions.assertEquals(BookingState.BOOKING_CONFIRMED, result.get().getStatus());
        Assertions.assertEquals("a@a.com", result.get().getRequesterEmail());
        Mockito.verify(mailService).sendBookingResolutionEmail(result.get());
    }

    @Test
    public void testConfirmPaymentReceivedUsesInactivePublicationForOwnerAuthorization() {
        final ItemBooking submittedBooking = new ItemBooking();
        submittedBooking.setId(30);
        submittedBooking.setItemId(20);
        submittedBooking.setGuestId(5);
        submittedBooking.setHostDecisionToken("t");
        submittedBooking.setRequestMessage("a");
        submittedBooking.setState(BookingState.BOOKING_PAYMENT_SUBMITTED);
        submittedBooking.setCreatedAt(OffsetDateTime.now());

        final ItemBooking paidBooking = new ItemBooking();
        paidBooking.setId(30);
        paidBooking.setItemId(20);
        paidBooking.setGuestId(5);
        paidBooking.setHostDecisionToken("t");
        paidBooking.setRequestMessage("a");
        paidBooking.setState(BookingState.BOOKING_PAID);
        paidBooking.setCreatedAt(submittedBooking.getCreatedAt());

        final Item inactiveItem = new Item();
        inactiveItem.setId(20);
        inactiveItem.setOwnerId(9);
        inactiveItem.setActive(false);
        inactiveItem.setTitle("Inactive item");

        final User requester = new User();
        requester.setId(5);
        requester.setGivenName("A");
        requester.setLastName("A");
        requester.setEmail("a@a.com");
        requester.setPreferredLanguage("es");

        final BookingPaymentProof proof = new BookingPaymentProof();
        proof.setBookingId(30);

        Mockito.when(itemDao.findBookingById(30))
                .thenReturn(Optional.of(submittedBooking))
                .thenReturn(Optional.of(paidBooking));
        Mockito.when(itemDao.findPaymentProofByBookingId(30)).thenReturn(Optional.of(proof));
        Mockito.when(itemDao.findAnyItemById(20)).thenReturn(Optional.of(inactiveItem));
        Mockito.when(itemDao.markBookingPaid(30, 9)).thenReturn(true);
        Mockito.when(itemDao.findUserById(5)).thenReturn(Optional.of(requester));

        final Optional<BookingRequest> result = bookingRequestService.confirmPaymentReceived(30, 9);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(BookingState.BOOKING_PAID, result.get().getStatus());
        Mockito.verify(mailService).sendPaymentReceivedEmail("a@a.com", "es", "Inactive item");
    }

    @Test
    public void testCreateBookingRequestWhenRequesterIsOwnerThrowsSelfBookingNotAllowed() {
        final User ownerUser = new User();
        ownerUser.setId(7);
        ownerUser.setGivenName("O");
        ownerUser.setLastName("O");
        ownerUser.setEmail("o@o.com");
        ownerUser.setPreferredLanguage("es");

        final Item item = new Item();
        item.setId(15);
        item.setOwnerId(7);

        Mockito.when(itemDao.findUserByEmail("o@o.com")).thenReturn(Optional.of(ownerUser));
        Mockito.when(itemDao.findAnyItemById(15)).thenReturn(Optional.of(item));

        final OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        final OffsetDateTime end = start.plusHours(2);

        Assertions.assertThrows(
                SelfBookingNotAllowedException.class,
                () -> bookingRequestService.createBookingRequest(15, "O", "O", "o@o.com", "es", start, end, "msg"));

        Mockito.verify(itemDao, Mockito.never())
                .createBookingRequest(
                        Mockito.anyInt(),
                        Mockito.anyInt(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyString(),
                        Mockito.anyString());
    }

    @Test
    public void testCreateOwnerSelfBlockInsertsWhenNoOverlap() {
        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(3);

        final OffsetDateTime start = OffsetDateTime.parse("2030-01-15T10:00:00+00:00");
        final OffsetDateTime end = OffsetDateTime.parse("2030-01-15T12:00:00+00:00");

        final ItemBooking inserted = new ItemBooking();
        inserted.setId(100);
        inserted.setItemId(10);
        inserted.setGuestId(3);
        inserted.setState(BookingState.BOOKING_CONFIRMED);

        Mockito.when(itemDao.findItemByIdForOwner(10, 3)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.listBookingsByItemId(10)).thenReturn(List.of());
        Mockito.when(itemDao.insertOwnerPersonalBlock(
                        Mockito.eq(10),
                        Mockito.eq(3),
                        Mockito.eq(start),
                        Mockito.eq(end),
                        Mockito.anyString(),
                        Mockito.any()))
                .thenReturn(inserted);

        final ItemBooking result = bookingRequestService.createOwnerSelfBlock(10, 3, start, end);
        Assertions.assertEquals(100, result.getId());
        Mockito.verify(itemDao)
                .insertOwnerPersonalBlock(
                        Mockito.eq(10),
                        Mockito.eq(3),
                        Mockito.eq(start),
                        Mockito.eq(end),
                        Mockito.anyString(),
                        Mockito.any());
    }

    @Test
    public void testCreateOwnerSelfBlockWhenGuestBookingOverlapsThrows() {
        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(3);

        final ItemBooking guestBooking = new ItemBooking();
        guestBooking.setGuestId(99);
        guestBooking.setState(BookingState.BOOKING_CONFIRMED);
        guestBooking.setStartTime(OffsetDateTime.parse("2030-01-15T10:00:00+00:00"));
        guestBooking.setEndTime(OffsetDateTime.parse("2030-01-15T11:00:00+00:00"));

        Mockito.when(itemDao.findItemByIdForOwner(10, 3)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.listBookingsByItemId(10)).thenReturn(List.of(guestBooking));

        final OffsetDateTime start = OffsetDateTime.parse("2030-01-15T10:30:00+00:00");
        final OffsetDateTime end = OffsetDateTime.parse("2030-01-15T12:00:00+00:00");

        Assertions.assertThrows(
                OverlappingActiveBookingException.class,
                () -> bookingRequestService.createOwnerSelfBlock(10, 3, start, end));

        Mockito.verify(itemDao, Mockito.never())
                .insertOwnerPersonalBlock(
                        Mockito.anyInt(),
                        Mockito.anyInt(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.anyString(),
                        Mockito.any());
    }

    @Test
    public void testRemoveOwnerSelfBlockWhenValidCancelsBooking() {
        final ItemBooking block = new ItemBooking();
        block.setId(55);
        block.setItemId(10);
        block.setGuestId(3);
        block.setState(BookingState.BOOKING_CONFIRMED);

        final Item item = new Item();
        item.setId(10);
        item.setOwnerId(3);

        Mockito.when(itemDao.findBookingById(55)).thenReturn(Optional.of(block));
        Mockito.when(itemDao.findAnyItemById(10)).thenReturn(Optional.of(item));
        Mockito.when(itemDao.markBookingCancelled(55)).thenReturn(true);

        Assertions.assertTrue(bookingRequestService.removeOwnerSelfBlock(55, 3));
        Mockito.verify(itemDao).markBookingCancelled(55);
    }

    @Test
    public void testRemoveOwnerSelfBlockWhenGuestMismatchReturnsFalse() {
        final ItemBooking block = new ItemBooking();
        block.setId(55);
        block.setItemId(10);
        block.setGuestId(99);
        block.setState(BookingState.BOOKING_CONFIRMED);

        Mockito.when(itemDao.findBookingById(55)).thenReturn(Optional.of(block));

        Assertions.assertFalse(bookingRequestService.removeOwnerSelfBlock(55, 3));
        Mockito.verify(itemDao, Mockito.never()).markBookingCancelled(Mockito.anyInt());
    }
}
