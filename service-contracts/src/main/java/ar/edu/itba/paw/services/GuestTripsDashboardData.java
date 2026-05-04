package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ItemBooking;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

@Getter
public final class GuestTripsDashboardData {

    private final Page<ItemBooking> sentBookingPage;
    private final Set<Integer> imageItemIds;
    private final Map<Integer, String> sentStatusMessageCodeByBookingId;
    private final Map<Integer, String> sentDateLabelByBookingId;
    private final Map<Integer, String> sentTimeRangeLabelByBookingId;
    private final Map<Integer, String> sentTotalPriceLabelByBookingId;
    private final Map<Integer, String> sentItemTitleByBookingId;
    private final Map<Integer, String> sentOwnerNameByBookingId;
    private final Map<Integer, String> sentOwnerEmailByBookingId;
    private final Map<Integer, String> sentPaymentAliasByBookingId;
    private final Map<Integer, String> sentPaymentRefusalReasonByBookingId;
    private final Map<Integer, Boolean> sentHasPaymentRefusalReasonByBookingId;
    private final Map<Integer, Boolean> sentPaymentProofPdfByBookingId;
    private final Map<Integer, Integer> sentBookedSnapshotVersionIdByBookingId;

    public GuestTripsDashboardData(
            final Page<ItemBooking> sentBookingPage,
            final Set<Integer> imageItemIds,
            final Map<Integer, String> sentStatusMessageCodeByBookingId,
            final Map<Integer, String> sentDateLabelByBookingId,
            final Map<Integer, String> sentTimeRangeLabelByBookingId,
            final Map<Integer, String> sentTotalPriceLabelByBookingId,
            final Map<Integer, String> sentItemTitleByBookingId,
            final Map<Integer, String> sentOwnerNameByBookingId,
            final Map<Integer, String> sentOwnerEmailByBookingId,
            final Map<Integer, String> sentPaymentAliasByBookingId,
            final Map<Integer, String> sentPaymentRefusalReasonByBookingId,
            final Map<Integer, Boolean> sentHasPaymentRefusalReasonByBookingId,
            final Map<Integer, Boolean> sentPaymentProofPdfByBookingId,
            final Map<Integer, Integer> sentBookedSnapshotVersionIdByBookingId) {
        this.sentBookingPage = sentBookingPage;
        this.imageItemIds = imageItemIds;
        this.sentStatusMessageCodeByBookingId = sentStatusMessageCodeByBookingId;
        this.sentDateLabelByBookingId = sentDateLabelByBookingId;
        this.sentTimeRangeLabelByBookingId = sentTimeRangeLabelByBookingId;
        this.sentTotalPriceLabelByBookingId = sentTotalPriceLabelByBookingId;
        this.sentItemTitleByBookingId = sentItemTitleByBookingId;
        this.sentOwnerNameByBookingId = sentOwnerNameByBookingId;
        this.sentOwnerEmailByBookingId = sentOwnerEmailByBookingId;
        this.sentPaymentAliasByBookingId = sentPaymentAliasByBookingId;
        this.sentPaymentRefusalReasonByBookingId = sentPaymentRefusalReasonByBookingId;
        this.sentHasPaymentRefusalReasonByBookingId = sentHasPaymentRefusalReasonByBookingId;
        this.sentPaymentProofPdfByBookingId = sentPaymentProofPdfByBookingId;
        this.sentBookedSnapshotVersionIdByBookingId = sentBookedSnapshotVersionIdByBookingId;
    }
}
