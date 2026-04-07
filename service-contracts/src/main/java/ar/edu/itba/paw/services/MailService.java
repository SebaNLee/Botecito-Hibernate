package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestSubmission;
import java.util.Locale;

public interface MailService {
    void sendTestConfirmationEmail(String recipientEmail);

    void sendPublishConfirmationEmail(String recipientEmail, String ownerName, String itemTitle);

    void sendRequestReviewEmail(RequestSubmission requestSubmission);

    void sendRequestResolutionEmail(RequestSubmission requestSubmission);

    Locale resolveLocale(String recipientIdentifier);
}
