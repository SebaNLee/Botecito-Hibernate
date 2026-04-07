package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import java.util.Locale;

public interface MailService {
    void sendTestConfirmationEmail(String recipientEmail);

    void sendPublishConfirmationEmail(
            String recipientEmail, String ownerName, String itemTitle, String ownerDeleteToken);

    void sendBookingReviewEmail(BookingRequest bookingRequest, String ownerEmail);

    void sendBookingResolutionEmail(BookingRequest bookingRequest);

    Locale resolveLocale(String recipientIdentifier);
}
