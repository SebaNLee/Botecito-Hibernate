package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.mail.MailRecipientModel;
import ar.edu.itba.paw.models.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.models.mail.PublishConfirmationMailModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service("nuevoMailService")
@SuppressFBWarnings(
        value = {"CT_CONSTRUCTOR_THROW"},
        justification =
                "Spring injects singleton collaborators and constructor validation fails fast on missing mail configuration.")
public class MailServiceImpl implements MailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final String myBoatsBaseUrl;
    private final String passwordRecoveryBaseUrl;
    private final String emailVerificationBaseUrl;

    public MailServiceImpl(
            final JavaMailSender mailSender,
            final TemplateEngine templateEngine,
            final MessageSource messageSource,
            @Qualifier("credentialsProperties") final Properties credentialsProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        final String baseUrl = requireProperty(credentialsProperties, "app.baseUrl");
        this.myBoatsBaseUrl = baseUrl + "/my-boats";
        this.passwordRecoveryBaseUrl = baseUrl + "/password-recovery";
        this.emailVerificationBaseUrl = baseUrl + "/verify-email";
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPublishConfirmationEmail(final PublishConfirmationMailModel mail) {
        if (mail == null || !hasEmail(mail.getOwner())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getOwner());
            final Context context = new Context(locale);
            context.setVariable("ownerName", mail.getOwner().getDisplayName());
            context.setVariable("itemTitle", mail.getItemTitle());
            context.setVariable("profileUrl", myBoatsBaseUrl);
            sendHtmlEmail(
                    mail.getOwner().getEmail(),
                    getMessage("mail.publishConfirmation.subject", locale, mail.getItemTitle()),
                    templateEngine.process("publish-confirmation", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send publish confirmation email for item title '{}'.", mail.getItemTitle(), e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPasswordRecoveryEmail(final PasswordRecoveryMailModel mail) {
        if (mail == null || !hasEmail(mail.getRecipient()) || isBlank(mail.getRecoveryToken())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getRecipient());
            final Context context = new Context(locale);
            context.setVariable("recipientName", mail.getRecipient().getDisplayName());
            context.setVariable("recoveryUrl", passwordRecoveryBaseUrl + "/" + mail.getRecoveryToken());
            sendHtmlEmail(
                    mail.getRecipient().getEmail(),
                    getMessage("mail.passwordRecovery.subject", locale),
                    templateEngine.process("password-recovery", context));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not send password recovery email to {}.",
                    mail.getRecipient().getEmail(),
                    e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendEmailVerificationEmail(final EmailVerificationMailModel mail) {
        if (mail == null || !hasEmail(mail.getRecipient()) || isBlank(mail.getVerificationToken())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getRecipient());
            final Context context = new Context(locale);
            context.setVariable("recipientName", mail.getRecipient().getDisplayName());
            context.setVariable("verificationUrl", emailVerificationBaseUrl + "/" + mail.getVerificationToken());
            sendHtmlEmail(
                    mail.getRecipient().getEmail(),
                    getMessage("mail.emailVerification.subject", locale),
                    templateEngine.process("email-verification", context));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not send email verification to {}.",
                    mail.getRecipient().getEmail(),
                    e);
        }
    }

    @Override
    public Locale resolveLocale(final MailRecipientModel recipient) {
        if (recipient == null || recipient.getPreferredLanguage() == null) {
            return Locale.of("es");
        }
        return recipient.getPreferredLanguage().toLocale();
    }

    private static boolean hasEmail(final MailRecipientModel recipient) {
        return recipient != null && !isBlank(recipient.getEmail());
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private void sendHtmlEmail(final String recipientEmail, final String subject, final String htmlBody) {
        sendHtmlEmail(recipientEmail, subject, htmlBody, null, null, null);
    }

    private void sendHtmlEmail(
            final String recipientEmail,
            final String subject,
            final String htmlBody,
            final byte[] inlineFileData,
            final String inlineContentType,
            final String inlineContentId) {
        try {
            final MimeMessage mimeMessage = mailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            if (inlineFileData != null
                    && inlineFileData.length > 0
                    && inlineContentType != null
                    && inlineContentId != null) {
                helper.addInline(inlineContentId, new ByteArrayResource(inlineFileData), inlineContentType);
            }
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
}
