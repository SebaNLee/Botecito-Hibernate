package ar.edu.itba.paw.services.dto;

import java.time.OffsetDateTime;
import java.util.Locale;

public record ReceivedBookingView(
        int id,
        int itemId,
        Integer imageId,
        String itemTitle,
        String requesterName,
        String requesterEmail,
        double requesterAverageRating,
        int requesterTotalReviews,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String dateLabel,
        String timeRangeLabel,
        String totalPriceLabel,
        String paymentAlias,
        String statusMessageCode,
        String paymentProofFileName,
        String paymentProofContentType,
        String paymentRefusalReason,
        String paymentGuestReply,
        String requestMessage) {

    public int getId() {
        return id;
    }

    public int getItemId() {
        return itemId;
    }

    public Integer getImageId() {
        return imageId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public double getRequesterAverageRating() {
        return requesterAverageRating;
    }

    public int getRequesterTotalReviews() {
        return requesterTotalReviews;
    }

    public boolean isRequesterHasReviews() {
        return requesterTotalReviews > 0;
    }

    public boolean getRequesterHasReviews() {
        return isRequesterHasReviews();
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getTimeRangeLabel() {
        return timeRangeLabel;
    }

    public String getTotalPriceLabel() {
        return totalPriceLabel;
    }

    public String getPaymentAlias() {
        return paymentAlias;
    }

    public String getStatusMessageCode() {
        return statusMessageCode;
    }

    public boolean isHasPaymentProof() {
        return paymentProofFileName != null && !paymentProofFileName.isBlank();
    }

    public boolean getHasPaymentProof() {
        return isHasPaymentProof();
    }

    public String getPaymentProofFileName() {
        return paymentProofFileName;
    }

    public String getPaymentProofContentType() {
        return paymentProofContentType;
    }

    public boolean getIsPaymentProofImage() {
        return paymentProofContentType != null
                && paymentProofContentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    public boolean getIsPaymentProofPdf() {
        return paymentProofContentType != null && "application/pdf".equalsIgnoreCase(paymentProofContentType);
    }

    public String getPaymentRefusalReason() {
        return paymentRefusalReason;
    }

    public boolean getHasPaymentRefusalReason() {
        return paymentRefusalReason != null && !paymentRefusalReason.isBlank();
    }

    public String getPaymentGuestReply() {
        return paymentGuestReply;
    }

    public boolean getHasPaymentGuestReply() {
        return paymentGuestReply != null && !paymentGuestReply.isBlank();
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public boolean getHasRequestMessage() {
        return requestMessage != null && !requestMessage.isBlank();
    }
}
