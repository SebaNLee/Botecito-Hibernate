package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingMailModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingResolutionMailModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingReviewMailModel;
import ar.edu.itba.paw.models.nuevo.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.nuevo.mail.MailRecipientModel;
import ar.edu.itba.paw.models.nuevo.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentProofRefusedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentProofSubmittedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentReceivedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PublishConfirmationMailModel;
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
    private final String reviewRecipient;
    private final String myBoatsBaseUrl;
    private final String requestsBaseUrl;
    private final String bookingsBaseUrl;
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
        this.reviewRecipient = requireProperty(credentialsProperties, "mail.reviewRecipient");
        this.myBoatsBaseUrl = baseUrl + "/my-boats";
        this.requestsBaseUrl = baseUrl + "/requests/incoming";
        this.bookingsBaseUrl = baseUrl + "/bookings";
        this.passwordRecoveryBaseUrl = baseUrl + "/password-recovery";
        this.emailVerificationBaseUrl = baseUrl + "/verify-email";
    }

    @Override
    public void sendTestConfirmationEmail(final MailRecipientModel recipient) {
        if (!hasEmail(recipient)) {
            return;
        }
        final Locale locale = resolveLocale(recipient);
        sendHtmlEmail(
                recipient.getEmail(),
                getMessage("mail.testConfirmation.subject", locale),
                templateEngine.process("test-confirmation", new Context(locale)));
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
    public void sendBookingReviewEmail(final BookingReviewMailModel mail) {
        if (mail == null) {
            return;
        }
        try {
            final MailRecipientModel recipient = hasEmail(mail.getOwner())
                    ? mail.getOwner()
                    : fallbackRecipient(reviewRecipient, PreferredLanguageModel.ES);
            final Locale locale = resolveLocale(recipient);
            final BookingMailModel booking = mail.getBooking();
            final Context context = new Context(locale);
            context.setVariable("bookingRequest", booking);
            context.setVariable("itemTitle", mail.getItemTitle());
            context.setVariable("location", mail.getLocation());
            context.setVariable("requestedDateLabel", mail.getRequestedDateLabel());
            context.setVariable("requestedTimeLabel", mail.getRequestedTimeLabel());
            final String message = booking == null || booking.getDescription() == null
                    ? ""
                    : booking.getDescription().trim();
            context.setVariable("hasRequestMessage", !message.isEmpty());
            context.setVariable("requestMessage", message);
            context.setVariable("profileUrl", requestsBaseUrl);
            sendHtmlEmail(
                    recipient.getEmail(),
                    getMessage("mail.requestReview.subject", locale, requesterName(mail)),
                    templateEngine.process("booking-review", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send booking review email.", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendBookingResolutionEmail(final BookingResolutionMailModel mail) {
        if (mail == null || !hasEmail(mail.getRequester()) || mail.getBooking() == null) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getRequester());
            final BookingMailModel booking = mail.getBooking();
            final Context context = new Context(locale);
            context.setVariable("bookingRequest", booking);
            context.setVariable("statusLabel", getMessage(statusMessageCode(booking), locale));
            context.setVariable("bookingUrl", bookingsBaseUrl + "#sent-booking-requests");
            sendHtmlEmail(
                    mail.getRequester().getEmail(),
                    getMessage(
                            "mail.requestResolution.subject", locale, getMessage(statusMessageCode(booking), locale)),
                    templateEngine.process("booking-resolution", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send booking resolution email.", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPaymentProofSubmittedEmail(final PaymentProofSubmittedMailModel mail) {
        if (mail == null || !hasEmail(mail.getOwner())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getOwner());
            final Context context = new Context(locale);
            context.setVariable("requesterName", mail.getRequesterName());
            context.setVariable("itemTitle", mail.getItemTitle());
            context.setVariable("profileUrl", requestsBaseUrl);
            final boolean hasProofImage = isInlineProofImage(mail.getProofFileData(), mail.getProofContentType());
            context.setVariable("hasProofImage", hasProofImage);
            if (hasProofImage) {
                context.setVariable("proofImageSrc", "cid:payment-proof-image");
            }

            final String htmlBody = templateEngine.process("payment-proof-submitted", context);
            sendHtmlEmail(
                    mail.getOwner().getEmail(),
                    getMessage("mail.paymentProofSubmitted.subject", locale, mail.getRequesterName()),
                    htmlBody,
                    hasProofImage ? mail.getProofFileData() : null,
                    hasProofImage ? mail.getProofContentType() : null,
                    hasProofImage ? "payment-proof-image" : null);
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send payment proof email.", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPaymentReceivedEmail(final PaymentReceivedMailModel mail) {
        if (mail == null || !hasEmail(mail.getRequester())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getRequester());
            final Context context = new Context(locale);
            context.setVariable("itemTitle", mail.getItemTitle());
            context.setVariable("profileUrl", bookingsBaseUrl + "#sent-booking-requests");
            sendHtmlEmail(
                    mail.getRequester().getEmail(),
                    getMessage("mail.paymentReceived.subject", locale, mail.getItemTitle()),
                    templateEngine.process("payment-received", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send payment received email.", e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPaymentProofRefusedEmail(final PaymentProofRefusedMailModel mail) {
        if (mail == null || !hasEmail(mail.getRequester())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(mail.getRequester());
            final Context context = new Context(locale);
            context.setVariable("ownerName", mail.getOwnerName());
            context.setVariable("itemTitle", mail.getItemTitle());
            context.setVariable("reason", mail.getReason());
            context.setVariable("profileUrl", bookingsBaseUrl + "#sent-booking-requests");
            sendHtmlEmail(
                    mail.getRequester().getEmail(),
                    getMessage("mail.paymentProofRefused.subject", locale, mail.getItemTitle()),
                    templateEngine.process("payment-proof-refused", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send payment proof refused email.", e);
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

    private static boolean isInlineProofImage(final byte[] proofFileData, final String proofContentType) {
        return proofFileData != null
                && proofFileData.length > 0
                && proofContentType != null
                && proofContentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private static MailRecipientModel fallbackRecipient(
            final String email, final PreferredLanguageModel preferredLanguage) {
        final MailRecipientModel recipient = new MailRecipientModel();
        recipient.setEmail(email);
        recipient.setDisplayName(email);
        recipient.setPreferredLanguage(preferredLanguage);
        return recipient;
    }

    private static String requesterName(final BookingReviewMailModel mail) {
        if (mail.getBooking() != null && !isBlank(mail.getBooking().getRequesterName())) {
            return mail.getBooking().getRequesterName();
        }
        if (mail.getRequester() != null && !isBlank(mail.getRequester().getDisplayName())) {
            return mail.getRequester().getDisplayName();
        }
        return "";
    }

    private static String statusMessageCode(final BookingMailModel booking) {
        if (booking == null || booking.getStatus() == null) {
            return "request.status.updated";
        }
        return "booking.status." + booking.getStatus().name();
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
