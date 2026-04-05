package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestSubmission;
import java.util.Locale;

public interface MailService {
    void sendTestConfirmationEmail(String recipientEmail, Locale locale);

    void sendRequestReviewEmail(RequestSubmission requestSubmission);

    void sendRequestResolutionEmail(RequestSubmission requestSubmission);
}
