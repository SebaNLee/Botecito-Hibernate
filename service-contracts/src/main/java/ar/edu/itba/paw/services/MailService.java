package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import java.util.Locale;

public interface MailService {
    void sendTestConfirmationEmail(String recipientEmail);

    void sendPublishConfirmationEmail(String recipientEmail, String ownerName, String itemTitle);

    void sendBookingReviewEmail(
            BookingRequest bookingRequest,
            String ownerEmail,
            String itemTitle,
            String location,
            String requestedDateLabel,
            String requestedTimeLabel);

    void sendBookingResolutionEmail(BookingRequest bookingRequest);

    void sendPaymentProofSubmittedEmail(
            String ownerEmail, String requesterName, String itemTitle, byte[] proofFileData, String proofContentType);

    void sendPaymentReceivedEmail(String requesterEmail, String requesterLocaleTag, String itemTitle);

    void sendPaymentProofRefusedEmail(
            String requesterEmail, String requesterLocaleTag, String ownerName, String itemTitle, String reason);

    void sendPasswordRecoveryEmail(String recipientEmail, String recipientName, String recoveryToken);

    Locale resolveLocale(String recipientIdentifier);
}
