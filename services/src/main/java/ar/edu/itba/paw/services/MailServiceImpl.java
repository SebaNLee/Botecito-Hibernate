package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private static final String BOTECITO_EMAIL = "botecito.dev@gmail.com";
    private static final String ACTION_BASE_URL = "http://localhost:8080/requests";

    private final JavaMailSender mailSender;

    public MailServiceImpl(final JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendTestConfirmationEmail(final String recipientEmail) {
        final SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);
        message.setSubject("Botecito test confirmation");
        message.setText("This is a test confirmation email sent from the Botecito development environment.");

        mailSender.send(message);
    }

    @Override
    public void sendRequestReviewEmail(final RequestSubmission requestSubmission) {
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(BOTECITO_EMAIL);
            helper.setSubject("New Botecito request from " + requestSubmission.getRequesterName());
            helper.setText(buildReviewEmailHtml(requestSubmission), true);
            mailSender.send(mimeMessage);
        } catch (final MessagingException e) {
            throw new IllegalStateException("Could not build the request review email.", e);
        }
    }

    @Override
    public void sendRequestResolutionEmail(final RequestSubmission requestSubmission) {
        final SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(requestSubmission.getRequesterEmail());
        message.setSubject("Your Botecito request was " + formatStatus(requestSubmission.getStatus()));
        message.setText(buildResolutionEmailText(requestSubmission));
        mailSender.send(message);
    }

    private String buildReviewEmailHtml(final RequestSubmission requestSubmission) {
        final String acceptUrl = ACTION_BASE_URL + "/" + requestSubmission.getToken() + "/accept";
        final String declineUrl = ACTION_BASE_URL + "/" + requestSubmission.getToken() + "/decline";

        return "<html>\n"
                + "  <body style=\"font-family: Arial, sans-serif; color: #0f172a;\">\n"
                + "    <h2>New request received</h2>\n"
                + "    <p><strong>Name:</strong> "
                + escapeHtml(requestSubmission.getRequesterName())
                + "</p>\n"
                + "    <p><strong>Email:</strong> "
                + escapeHtml(requestSubmission.getRequesterEmail())
                + "</p>\n"
                + "    <p><strong>Description:</strong></p>\n"
                + "    <p style=\"white-space: pre-wrap;\">"
                + escapeHtml(requestSubmission.getDescription())
                + "</p>\n"
                + "    <p style=\"margin-top: 24px;\">\n"
                + "      <a href=\""
                + acceptUrl
                + "\" style=\"display: inline-block; margin-right: 12px; padding: 12px 20px; border-radius: 8px; background: #059669; color: #ffffff; text-decoration: none; font-weight: 600;\">Accept</a>\n"
                + "      <a href=\""
                + declineUrl
                + "\" style=\"display: inline-block; padding: 12px 20px; border-radius: 8px; background: #dc2626; color: #ffffff; text-decoration: none; font-weight: 600;\">Decline</a>\n"
                + "    </p>\n"
                + "  </body>\n"
                + "</html>\n";
    }

    private String buildResolutionEmailText(final RequestSubmission requestSubmission) {
        return "Hello "
                + requestSubmission.getRequesterName()
                + ",\n\n"
                + "Your request to Botecito was "
                + formatStatus(requestSubmission.getStatus())
                + ".\n\n"
                + "Description:\n"
                + requestSubmission.getDescription()
                + "\n";
    }

    private String formatStatus(final RequestStatus status) {
        return switch (status) {
            case ACCEPTED -> "accepted";
            case DECLINED -> "declined";
            default -> "updated";
        };
    }

    private String escapeHtml(final String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
