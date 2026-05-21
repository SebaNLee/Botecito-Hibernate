package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PreferredLanguageModel;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
    private final String appBaseUrl;
    private final String myBoatsBaseUrl;
    private final String incomingBookingsBaseUrl;
    private final String outgoingBookingsBaseUrl;
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
        this.appBaseUrl = baseUrl;
        this.myBoatsBaseUrl = baseUrl + "/my-boats";
        this.incomingBookingsBaseUrl = baseUrl + "/requests/incoming";
        this.outgoingBookingsBaseUrl = baseUrl + "/requests/outgoing";
        this.passwordRecoveryBaseUrl = baseUrl + "/password-recovery";
        this.emailVerificationBaseUrl = baseUrl + "/verify-email";
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPublishConfirmationEmail(final Version version) {
        final Users owner = publishOwner(version);
        if (version == null || !hasEmail(owner)) {
            return;
        }
        try {
            final Locale locale = resolveLocale(owner);
            final Context context = new Context(locale);
            context.setVariable("ownerName", displayName(owner));
            context.setVariable("itemTitle", version.getTitle());
            context.setVariable("profileUrl", myBoatsBaseUrl);
            sendHtmlEmail(
                    owner.getEmail(),
                    getMessage("mail.publishConfirmation.subject", locale, version.getTitle()),
                    templateEngine.process("publish-confirmation", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send publish confirmation email for item title '{}'.", version.getTitle(), e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendFollowerPublishNotificationEmail(final Users subscriber, final Version version) {
        final Users owner = publishOwner(version);
        if (version == null || owner == null || !hasEmail(subscriber)) {
            return;
        }
        try {
            final Locale locale = resolveLocale(subscriber);
            final Context context = new Context(locale);
            context.setVariable("recipientName", displayName(subscriber));
            context.setVariable("ownerName", displayName(owner));
            context.setVariable("itemTitle", version.getTitle());
            context.setVariable("itemUrl", itemUrl(version));
            sendHtmlEmail(
                    subscriber.getEmail(),
                    getMessage("mail.followerPublish.subject", locale, displayName(owner), version.getTitle()),
                    templateEngine.process("follower-publish-notification", context));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not send follower publish notification for item title '{}' to {}.",
                    version.getTitle(),
                    subscriber.getEmail(),
                    e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPasswordRecoveryEmail(final Users user) {
        if (user == null || !hasEmail(user) || isBlank(user.getMailToken())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(user);
            final Context context = new Context(locale);
            context.setVariable("recipientName", displayName(user));
            context.setVariable("recoveryUrl", passwordRecoveryBaseUrl + "/" + user.getMailToken());
            sendHtmlEmail(
                    user.getEmail(),
                    getMessage("mail.passwordRecovery.subject", locale),
                    templateEngine.process("password-recovery", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send password recovery email to {}.", user.getEmail(), e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendEmailVerificationEmail(final Users user) {
        if (user == null || !hasEmail(user) || isBlank(user.getMailToken())) {
            return;
        }
        try {
            final Locale locale = resolveLocale(user);
            final Context context = new Context(locale);
            context.setVariable("recipientName", displayName(user));
            context.setVariable("verificationUrl", emailVerificationBaseUrl + "/" + user.getMailToken());
            sendHtmlEmail(
                    user.getEmail(),
                    getMessage("mail.emailVerification.subject", locale),
                    templateEngine.process("email-verification", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send email verification to {}.", user.getEmail(), e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPreBookingMail(final Booking booking) {
        sendBookingEmail(booking, host(booking), "mail.booking.preBooking", incomingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendAcceptMail(final Booking booking) {
        sendBookingEmail(booking, guest(booking), "mail.booking.accepted", outgoingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRejectMail(final Booking booking) {
        sendBookingEmail(booking, guest(booking), "mail.booking.rejected", outgoingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPaymentMail(final Booking booking) {
        sendBookingEmail(booking, host(booking), "mail.booking.payment", incomingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendRefusedPaymentMail(final Booking booking) {
        sendBookingEmail(booking, guest(booking), "mail.booking.paymentRefused", outgoingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendBookingConfirmedMail(final Booking booking) {
        sendBookingEmail(booking, guest(booking), "mail.booking.confirmed", outgoingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendBookingCancelledMail(final Booking booking) {
        sendBookingEmail(booking, host(booking), "mail.booking.cancelled", incomingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendBookingExpiredMail(final Booking booking) {
        sendBookingEmail(booking, guest(booking), "mail.booking.expired", outgoingBookingsBaseUrl);
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendBookingFinishedMail(final Booking booking) {
        sendBookingEmail(booking, guest(booking), "mail.booking.finished", outgoingBookingsBaseUrl);
        sendBookingEmail(booking, host(booking), "mail.booking.finished", incomingBookingsBaseUrl);
    }

    public Locale resolveLocale(final Users user) {
        return user == null
                ? Locale.of("es")
                : PreferredLanguageModel.fromPersistence(user.getLanguage()).toLocale();
    }

    private static boolean hasEmail(final Users user) {
        return user != null && !isBlank(user.getEmail());
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private void sendBookingEmail(
            final Booking booking, final Users recipient, final String messagePrefix, final String url) {
        if (booking == null || !hasEmail(recipient)) {
            return;
        }
        try {
            final Locale locale = resolveLocale(recipient);
            final Version version = booking.getVersion();
            final PaymentProof proof = booking.getPaymentProof();
            final String timezone = timezone(booking);
            final String bookingDate = formatBookingRange(booking.getStart(), booking.getEnd(), timezone, locale);
            final Context context = new Context(locale);
            context.setVariable("recipientName", displayName(recipient));
            context.setVariable("itemTitle", version.getTitle());
            context.setVariable("bookingId", booking.getId());
            context.setVariable("bookingDate", bookingDate);
            context.setVariable("timezone", timezone);
            context.setVariable("message", booking.getMsg());
            context.setVariable("paymentReply", proof == null ? null : proof.getReplyMsg());
            context.setVariable("refusalReason", proof == null ? null : proof.getRefuseMsg());
            context.setVariable("actionUrl", url);
            context.setVariable("heading", getMessage(messagePrefix + ".heading", locale));
            context.setVariable("body", getMessage(messagePrefix + ".body", locale, version.getTitle(), bookingDate));
            context.setVariable("note", getMessage(messagePrefix + ".note", locale));
            context.setVariable("action", getMessage(messagePrefix + ".action", locale));
            sendHtmlEmail(
                    recipient.getEmail(),
                    getMessage(messagePrefix + ".subject", locale, version.getTitle()),
                    templateEngine.process("booking-lifecycle", context));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not send booking email '{}' for booking {} to {}.",
                    messagePrefix,
                    booking.getId(),
                    recipient.getEmail(),
                    e);
        }
    }

    private static Users guest(final Booking booking) {
        return booking == null ? null : booking.getGuest();
    }

    private static Users host(final Booking booking) {
        if (booking == null) {
            return null;
        }
        final Version version = booking.getVersion();
        final Item item = version == null ? null : version.getItem();
        return item == null ? null : item.getHost();
    }

    private static Users publishOwner(final Version version) {
        if (version == null || version.getItem() == null) {
            return null;
        }
        return version.getItem().getHost();
    }

    private String itemUrl(final Version version) {
        if (version == null || version.getItem() == null || version.getItem().getId() == null) {
            return appBaseUrl + "/marketplace";
        }
        return appBaseUrl + "/item/" + version.getItem().getId();
    }

    private static String displayName(final Users user) {
        if (user == null) {
            return null;
        }
        final String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "");
        final String trimmed = fullName.trim();
        return trimmed.isBlank() ? user.getEmail() : trimmed;
    }

    private static String timezone(final Booking booking) {
        final Version version = booking.getVersion();
        final String timezone = version == null ? null : version.getTimezone();
        return isBlank(timezone) ? "UTC" : timezone;
    }

    private static String formatBookingRange(
            final LocalDateTime utcStart, final LocalDateTime utcEnd, final String timezone, final Locale locale) {
        if (utcStart == null || utcEnd == null) {
            return "";
        }
        final ZoneId zone = ZoneId.of(isBlank(timezone) ? "UTC" : timezone.trim());
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", locale);
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale);
        final var localStart = utcStart.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
        final var localEnd = utcEnd.atZone(ZoneOffset.UTC).withZoneSameInstant(zone);
        if (localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            return dateFormatter.format(localStart) + " - " + timeFormatter.format(localEnd);
        }
        return dateFormatter.format(localStart) + " - " + dateFormatter.format(localEnd);
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
