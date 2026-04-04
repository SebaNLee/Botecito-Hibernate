package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailServiceImpl implements MailService {

    private static final String BOTECITO_EMAIL = "botecito.dev@gmail.com";
    private static final String ACTION_BASE_URL = "http://localhost:8080/requests";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring injects shared singleton collaborators here; MailServiceImpl does not expose them.")
    public MailServiceImpl(final JavaMailSender mailSender, final TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendTestConfirmationEmail(final String recipientEmail) {
        sendHtmlEmail(
                recipientEmail,
                "Botecito test confirmation",
                templateEngine.process("test-confirmation", new Context()));
    }

    @Override
    public void sendRequestReviewEmail(final RequestSubmission requestSubmission) {
        final Context context = new Context();
        context.setVariable("request", requestSubmission);
        context.setVariable("acceptUrl", ACTION_BASE_URL + "/" + requestSubmission.getToken() + "/accept");
        context.setVariable("declineUrl", ACTION_BASE_URL + "/" + requestSubmission.getToken() + "/decline");
        sendHtmlEmail(
                BOTECITO_EMAIL,
                "New Botecito request from " + requestSubmission.getRequesterName(),
                templateEngine.process("request-review", context));
    }

    @Override
    public void sendRequestResolutionEmail(final RequestSubmission requestSubmission) {
        final Context context = new Context();
        context.setVariable("request", requestSubmission);
        context.setVariable("statusLabel", formatStatus(requestSubmission.getStatus()));
        sendHtmlEmail(
                requestSubmission.getRequesterEmail(),
                "Your Botecito request was " + formatStatus(requestSubmission.getStatus()),
                templateEngine.process("request-resolution", context));
    }

    private void sendHtmlEmail(final String recipientEmail, final String subject, final String htmlBody) {
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
        } catch (final MessagingException e) {
            throw new IllegalStateException("Could not build the email message.", e);
        }
    }

    private String formatStatus(final RequestStatus status) {
        return switch (status) {
            case ACCEPTED -> "accepted";
            case DECLINED -> "declined";
            default -> "updated";
        };
    }
}
