package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.ItemSnapshot;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.util.BookingDisplayFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class BookingDashboardServiceImpl implements BookingDashboardService {

    private final ItemService itemService;
    private final BookingRequestService bookingRequestService;

    @Override
    public GuestTripsDashboardData loadGuestTripsDashboard(final int guestId, final int page, final int pageSize) {
        final Page<ItemBooking> sentBookingPage =
                Page.slice(itemService.listBookingsByGuestId(guestId), page, pageSize);
        final Set<Integer> imageItemIds = new LinkedHashSet<>();
        final Map<Integer, String> sentStatusMessageCodeByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentDateLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentTimeRangeLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentTotalPriceLabelByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentItemTitleByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentOwnerNameByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentOwnerEmailByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentPaymentAliasByBookingId = new LinkedHashMap<>();
        final Map<Integer, String> sentPaymentRefusalReasonByBookingId = new LinkedHashMap<>();
        final Map<Integer, Boolean> sentHasPaymentRefusalReasonByBookingId = new LinkedHashMap<>();
        final Map<Integer, Boolean> sentPaymentProofPdfByBookingId = new LinkedHashMap<>();
        final Map<Integer, Integer> sentBookedSnapshotVersionIdByBookingId = new LinkedHashMap<>();

        for (final ItemBooking booking : sentBookingPage.getContent()) {
            if (booking != null && booking.getItemId() != null) {
                imageItemIds.add(booking.getItemId());
            }
            if (booking == null || booking.getId() == null) {
                continue;
            }

            final Integer bookingId = booking.getId();
            final Item item = booking.getItemId() == null
                    ? null
                    : itemService.findItemById(booking.getItemId()).orElse(null);
            final Optional<ItemSnapshot> bookedPublication =
                    itemService.findSnapshotByBookingIdForGuest(bookingId, guestId);
            final User owner = item != null && item.getOwnerId() != null
                    ? itemService.findUserById(item.getOwnerId()).orElse(null)
                    : null;
            final Integer pricePerHour = bookedPublication
                    .map(ItemSnapshot::getPricePerHour)
                    .orElse(item == null ? null : item.getPricePerHour());
            final var paymentProof =
                    bookingRequestService.findPaymentProofByBookingId(bookingId).orElse(null);
            final String contentType = paymentProof == null ? null : paymentProof.getContentType();
            final String refusalReason = paymentProof == null ? null : paymentProof.getRefusalReason();

            sentStatusMessageCodeByBookingId.put(
                    bookingId,
                    booking.getState() == null ? "" : BookingDisplayFormatter.statusMessageCode(booking.getState()));
            sentDateLabelByBookingId.put(bookingId, BookingDisplayFormatter.formatDateLabel(booking.getStartTime()));
            sentTimeRangeLabelByBookingId.put(
                    bookingId,
                    BookingDisplayFormatter.formatTimeRangeLabel(booking.getStartTime(), booking.getEndTime()));
            sentTotalPriceLabelByBookingId.put(
                    bookingId,
                    BookingDisplayFormatter.formatTotalPriceLabel(
                            booking.getStartTime(), booking.getEndTime(), pricePerHour));
            sentItemTitleByBookingId.put(
                    bookingId,
                    bookedPublication.map(ItemSnapshot::getTitle).orElse(item == null ? "" : item.getTitle()));
            sentOwnerNameByBookingId.put(bookingId, owner == null ? "" : owner.getName());
            sentOwnerEmailByBookingId.put(bookingId, owner == null ? "" : owner.getEmail());
            sentPaymentAliasByBookingId.put(
                    bookingId, owner == null ? "" : BookingDisplayFormatter.resolvePaymentAlias(owner));
            sentPaymentRefusalReasonByBookingId.put(bookingId, refusalReason);
            sentHasPaymentRefusalReasonByBookingId.put(bookingId, refusalReason != null && !refusalReason.isBlank());
            sentPaymentProofPdfByBookingId.put(bookingId, "application/pdf".equalsIgnoreCase(contentType));
            sentBookedSnapshotVersionIdByBookingId.put(
                    bookingId, bookedPublication.map(ItemSnapshot::getVersionId).orElse(null));
        }

        return new GuestTripsDashboardData(
                sentBookingPage,
                imageItemIds,
                sentStatusMessageCodeByBookingId,
                sentDateLabelByBookingId,
                sentTimeRangeLabelByBookingId,
                sentTotalPriceLabelByBookingId,
                sentItemTitleByBookingId,
                sentOwnerNameByBookingId,
                sentOwnerEmailByBookingId,
                sentPaymentAliasByBookingId,
                sentPaymentRefusalReasonByBookingId,
                sentHasPaymentRefusalReasonByBookingId,
                sentPaymentProofPdfByBookingId,
                sentBookedSnapshotVersionIdByBookingId);
    }
}
