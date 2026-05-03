package ar.edu.itba.paw.services.dto;

import java.time.OffsetDateTime;
import java.util.Locale;

public record SentBookingView(
        int id,
        int itemId,
        Integer bookedSnapshotVersionId,
        Integer imageId,
        String itemTitle,
        String ownerName,
        String ownerEmail,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String dateLabel,
        String timeRangeLabel,
        String totalPriceLabel,
        String paymentAlias,
        String statusMessageCode,
        String paymentProofContentType,
        String paymentRefusalReason,
        String paymentGuestReply) {

    public int getId() {
        return id;
    }

    public int getItemId() {
        return itemId;
    }

    public Integer getBookedSnapshotVersionId() {
        return bookedSnapshotVersionId;
    }

    public Integer getImageId() {
        return imageId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
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
}
