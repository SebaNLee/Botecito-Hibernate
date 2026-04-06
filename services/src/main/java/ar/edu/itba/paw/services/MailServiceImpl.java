package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final String reviewRecipient;
    private final String actionBaseUrl;
    private final String itemBaseUrl;

    @SuppressFBWarnings(
            value = {"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"},
            justification = "Spring injects shared singleton collaborators here; constructor validation fails fast on"
                    + " missing mail configuration and does not expose partially initialized state.")
    public MailServiceImpl(
            final JavaMailSender mailSender,
            final TemplateEngine templateEngine,
            final MessageSource messageSource,
            final Properties credentialsProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        final String baseUrl = requireProperty(credentialsProperties, "app.baseUrl");
        this.reviewRecipient = requireProperty(credentialsProperties, "mail.reviewRecipient");
        this.actionBaseUrl = baseUrl + "/requests";
        this.itemBaseUrl = baseUrl + "/item";
    }

    @Override
    public void sendTestConfirmationEmail(final String recipientEmail) {
        final Locale locale = resolveLocale(recipientEmail);
        sendHtmlEmail(
                recipientEmail,
                getMessage("mail.testConfirmation.subject", locale),
                templateEngine.process("test-confirmation", new Context(locale)));
    }

    @Override
    public void sendRequestReviewEmail(final RequestSubmission requestSubmission) {
        final Locale locale = resolveLocale(reviewRecipient);
        final Context context = new Context(locale);
        context.setVariable("request", requestSubmission);
        context.setVariable("acceptUrl", actionBaseUrl + "/" + requestSubmission.getToken() + "/accept");
        context.setVariable("declineUrl", actionBaseUrl + "/" + requestSubmission.getToken() + "/decline");
        sendHtmlEmail(
                reviewRecipient,
                getMessage("mail.requestReview.subject", locale, requestSubmission.getRequesterName()),
                templateEngine.process("request-review", context));
    }

    @Override
    public void sendRequestResolutionEmail(final RequestSubmission requestSubmission) {
        final Locale locale = requestSubmission.getRequesterLocale();
        final Context context = new Context(locale);
        context.setVariable("request", requestSubmission);
        context.setVariable("statusLabel", getMessage(statusMessageCode(requestSubmission.getStatus()), locale));
        if (requestSubmission.getItemId() != null) {
            context.setVariable("itemUrl", itemBaseUrl + "/" + requestSubmission.getItemId());
        }
        sendHtmlEmail(
                requestSubmission.getRequesterEmail(),
                getMessage(
                        "mail.requestResolution.subject",
                        locale,
                        getMessage(statusMessageCode(requestSubmission.getStatus()), locale)),
                templateEngine.process("request-resolution", context));
    }

    @Override
    public Locale resolveLocale(final String recipientIdentifier) {
        // TODO load the user's preferred language from persistence once that data is available.
        return new Locale("es");
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

    private String getMessage(final String code, final Locale locale, final Object... args) {
        return messageSource.getMessage(code, args, locale);
    }

    private static String requireProperty(final Properties properties, final String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or blank '" + key + "' in credentials properties");
        }
        return value;
    }

    private String statusMessageCode(final RequestStatus status) {
        return switch (status) {
            case ACCEPTED -> "request.status.accepted";
            case DECLINED -> "request.status.declined";
            default -> "request.status.updated";
        };
    }
}
