package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestSubmission;

public interface MailService {
    void sendTestConfirmationEmail(String recipientEmail);

    void sendRequestReviewEmail(RequestSubmission requestSubmission);

    void sendRequestResolutionEmail(RequestSubmission requestSubmission);
}
