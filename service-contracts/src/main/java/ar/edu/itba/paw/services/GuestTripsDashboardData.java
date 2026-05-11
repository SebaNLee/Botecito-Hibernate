package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ItemBooking;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
    private final Map<Integer, Boolean> sentCanReviewByBookingId;
}
