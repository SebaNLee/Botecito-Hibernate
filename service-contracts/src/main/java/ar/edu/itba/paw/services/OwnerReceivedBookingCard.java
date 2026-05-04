package ar.edu.itba.paw.services;

import lombok.Getter;

@Getter
public final class OwnerReceivedBookingCard {

    private final Integer id;
    private final Integer itemId;
    private final String itemTitle;
    private final String statusMessageCode;
    private final String dateLabel;
    private final String timeRangeLabel;
    private final String totalPriceLabel;
    private final String paymentAlias;
    private final String requestMessage;
    private final boolean hasRequestMessage;
    private final String requesterName;
    private final String requesterEmail;
    private final boolean requesterHasReviews;
    private final double requesterAverageRating;
    private final int requesterTotalReviews;
    private final boolean hasPaymentGuestReply;
    private final String paymentGuestReply;
    private final boolean hasPaymentProof;
    private final boolean paymentProofPdf;
    private final String paymentRefusalReason;
    private final boolean hasPaymentRefusalReason;

    public OwnerReceivedBookingCard(
            final Integer id,
            final Integer itemId,
            final String itemTitle,
            final String statusMessageCode,
            final String dateLabel,
            final String timeRangeLabel,
            final String totalPriceLabel,
            final String paymentAlias,
            final String requestMessage,
            final boolean hasRequestMessage,
            final String requesterName,
            final String requesterEmail,
            final boolean requesterHasReviews,
            final double requesterAverageRating,
            final int requesterTotalReviews,
            final boolean hasPaymentGuestReply,
            final String paymentGuestReply,
            final boolean hasPaymentProof,
            final boolean paymentProofPdf,
            final String paymentRefusalReason,
            final boolean hasPaymentRefusalReason) {
        this.id = id;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.statusMessageCode = statusMessageCode;
        this.dateLabel = dateLabel;
        this.timeRangeLabel = timeRangeLabel;
        this.totalPriceLabel = totalPriceLabel;
        this.paymentAlias = paymentAlias;
        this.requestMessage = requestMessage;
        this.hasRequestMessage = hasRequestMessage;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requesterHasReviews = requesterHasReviews;
        this.requesterAverageRating = requesterAverageRating;
        this.requesterTotalReviews = requesterTotalReviews;
        this.hasPaymentGuestReply = hasPaymentGuestReply;
        this.paymentGuestReply = paymentGuestReply;
        this.hasPaymentProof = hasPaymentProof;
        this.paymentProofPdf = paymentProofPdf;
        this.paymentRefusalReason = paymentRefusalReason;
        this.hasPaymentRefusalReason = hasPaymentRefusalReason;
    }
}
