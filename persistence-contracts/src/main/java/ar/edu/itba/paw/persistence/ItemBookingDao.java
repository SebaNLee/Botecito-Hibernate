package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.BookingPaymentProof;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemBookingDao {

    List<ItemBooking> listBookings();

    List<ItemBooking> listBookingsByItemId(int itemId);

    List<ItemBooking> listBookingsByGuestId(int guestId);

    List<ItemBooking> listBookingsByOwnerId(int ownerId);

    List<ItemBooking> listPendingBookingsByOwnerId(int ownerId);

    List<ItemBooking> listPaymentSubmittedBookingsByOwnerId(int ownerId);

    List<ItemBooking> listActiveBookingsByItemId(int itemId);

    Optional<ItemBooking> findBookingByHostDecisionToken(String hostDecisionToken);

    List<ItemBooking> findBookingsByHostDecisionTokens(Collection<String> hostDecisionTokens);

    Optional<ItemBooking> findBookingById(int bookingId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForGuest(int bookingId, int guestId);

    Optional<ItemSnapshot> findSnapshotByBookingIdForOwner(int bookingId, int ownerId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForGuest(int versionId, int itemId, int guestId);

    Optional<ItemSnapshot> findSnapshotVersionByIdForOwner(int versionId, int itemId, int ownerId);

    List<ItemSnapshot> listSnapshotsByItemIdForGuest(int itemId, int guestId);

    List<ItemSnapshot> listSnapshotsByItemIdForOwner(int itemId, int ownerId);

    ItemBooking createBookingRequest(
            int itemId,
            int guestId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String requestMessage,
            String hostDecisionToken);

    ItemBooking insertOwnerPersonalBlock(
            int itemId,
            int ownerId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String hostDecisionToken,
            OffsetDateTime hostDecisionRecordedAt);

    boolean markBookingCancelled(int bookingId);

    boolean resolveBookingByHostDecisionToken(
            String hostDecisionToken, BookingState newState, OffsetDateTime hostDecisionUsedAt);

    void expireAllDueBookings(OffsetDateTime startTimeThreshold);

    int resolveBookingsByHostDecisionTokens(
            Collection<String> hostDecisionTokens, BookingState newState, OffsetDateTime hostDecisionUsedAt);

    BookingPaymentProof createPaymentProof(
            int bookingId, int uploaderId, String fileName, String contentType, byte[] fileData, String guestReply);

    Optional<BookingPaymentProof> findPaymentProofByBookingId(int bookingId);

    Optional<BookingPaymentProof> findPaymentProofById(int proofId);

    boolean deletePaymentProofByBookingId(int bookingId);

    boolean markBookingPaymentSubmitted(int bookingId, int guestId);

    boolean markBookingPaymentResubmitted(int bookingId, int guestId);

    boolean markBookingPaid(int bookingId, int ownerId);

    boolean markBookingPaymentRefused(int bookingId, int ownerId, String reason);
}
