package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailServiceImpl implements MailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MessageSource messageSource;
    private final ItemDao itemDao;
    private final String reviewRecipient;
    private final String accountBaseUrl;
    private final String itemBaseUrl;

    @SuppressFBWarnings(
            value = {"EI_EXPOSE_REP2", "CT_CONSTRUCTOR_THROW"},
            justification = "Spring injects shared singleton collaborators here; constructor validation fails fast on"
                    + " missing mail configuration and does not expose partially initialized state.")
    public MailServiceImpl(
            final JavaMailSender mailSender,
            final TemplateEngine templateEngine,
            final MessageSource messageSource,
            final ItemDao itemDao,
            final Properties credentialsProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.itemDao = itemDao;
        final String baseUrl = requireProperty(credentialsProperties, "app.baseUrl");
        this.reviewRecipient = requireProperty(credentialsProperties, "mail.reviewRecipient");
        this.accountBaseUrl = baseUrl + "/profile";
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
    @Async("mailTaskExecutor")
    public void sendPublishConfirmationEmail(
            final String recipientEmail, final String ownerName, final String itemTitle) {
        try {
            final Locale locale = resolveLocale(recipientEmail);
            final Context context = new Context(locale);
            context.setVariable("ownerName", ownerName);
            context.setVariable("itemTitle", itemTitle);
            context.setVariable("profileUrl", accountBaseUrl);
            sendHtmlEmail(
                    recipientEmail,
                    getMessage("mail.publishConfirmation.subject", locale, itemTitle),
                    templateEngine.process("publish-confirmation", context));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not send publish confirmation email to {} for item title '{}'.",
                    recipientEmail,
                    itemTitle,
                    e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendBookingReviewEmail(final BookingRequest bookingRequest, final String ownerEmail) {
        try {
            final String recipientEmail;
            if (ownerEmail != null && !ownerEmail.isBlank()) {
                recipientEmail = ownerEmail;
            } else {
                recipientEmail = resolveBookingReviewRecipient(bookingRequest).orElse(reviewRecipient);
            }
            final Locale locale = resolveLocale(recipientEmail);
            final Context context = new Context(locale);
            context.setVariable("bookingRequest", bookingRequest);
            context.setVariable("profileUrl", accountBaseUrl);
            sendHtmlEmail(
                    recipientEmail,
                    getMessage("mail.requestReview.subject", locale, bookingRequest.getRequesterName()),
                    templateEngine.process("booking-review", context));
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Could not send booking review email for booking token {} and requester {}.",
                    bookingRequest.getToken(),
                    bookingRequest.getRequesterEmail(),
                    e);
        }
    }

    @Override
    public void sendBookingResolutionEmail(final BookingRequest bookingRequest) {
        final Locale locale = bookingRequest.getRequesterLocale();
        final Context context = new Context(locale);
        context.setVariable("bookingRequest", bookingRequest);
        context.setVariable("statusLabel", getMessage(statusMessageCode(bookingRequest.getStatus()), locale));
        if (bookingRequest.getItemId() != null) {
            context.setVariable("itemUrl", itemBaseUrl + "/" + bookingRequest.getItemId());
        }
        sendHtmlEmail(
                bookingRequest.getRequesterEmail(),
                getMessage(
                        "mail.requestResolution.subject",
                        locale,
                        getMessage(statusMessageCode(bookingRequest.getStatus()), locale)),
                templateEngine.process("booking-resolution", context));
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPaymentProofSubmittedEmail(
            final String ownerEmail,
            final String requesterName,
            final String itemTitle,
            final byte[] proofFileData,
            final String proofContentType) {
        if (ownerEmail == null || ownerEmail.isBlank()) {
            return;
        }
        try {
            final Locale locale = resolveLocale(ownerEmail);
            final Context context = new Context(locale);
            context.setVariable("requesterName", requesterName);
            context.setVariable("itemTitle", itemTitle);
            context.setVariable("profileUrl", accountBaseUrl + "#received-booking-requests");
            final boolean hasProofImage = isInlineProofImage(proofFileData, proofContentType);
            context.setVariable("hasProofImage", hasProofImage);
            if (hasProofImage) {
                context.setVariable("proofImageSrc", "cid:payment-proof-image");
            }

            final String htmlBody = templateEngine.process("payment-proof-submitted", context);
            sendHtmlEmail(
                    ownerEmail,
                    getMessage("mail.paymentProofSubmitted.subject", locale, requesterName),
                    htmlBody,
                    hasProofImage ? proofFileData : null,
                    hasProofImage ? proofContentType : null,
                    hasProofImage ? "payment-proof-image" : null);
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send payment proof email to {}.", ownerEmail, e);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendPaymentReceivedEmail(
            final String requesterEmail, final String requesterLocaleTag, final String itemTitle) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            return;
        }
        try {
            final Locale locale = toSupportedLocale(requesterLocaleTag);
            final Context context = new Context(locale);
            context.setVariable("itemTitle", itemTitle);
            context.setVariable("profileUrl", accountBaseUrl + "#sent-booking-requests");
            sendHtmlEmail(
                    requesterEmail,
                    getMessage("mail.paymentReceived.subject", locale, itemTitle),
                    templateEngine.process("payment-received", context));
        } catch (final RuntimeException e) {
            LOGGER.error("Could not send payment received email to {}.", requesterEmail, e);
        }
    }

    @Override
    public Locale resolveLocale(final String recipientIdentifier) {
        final Optional<Locale> userLocale = itemDao.findUserByEmail(recipientIdentifier)
                .map(user -> toSupportedLocale(user.getPreferredLanguage()));
        if (userLocale.isPresent()) {
            return userLocale.get();
        }
        return Locale.of("es");
    }

    private static Locale toSupportedLocale(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return Locale.ENGLISH;
        }
        return Locale.of("es");
    }

    private static boolean isInlineProofImage(final byte[] proofFileData, final String proofContentType) {
        return proofFileData != null
                && proofFileData.length > 0
                && proofContentType != null
                && proofContentType.toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private Optional<String> resolveBookingReviewRecipient(final BookingRequest bookingRequest) {
        if (bookingRequest.getItemId() == null) {
            return Optional.empty();
        }

        final Optional<Item> item = itemDao.findItemById(bookingRequest.getItemId());
        if (item.isEmpty()) {
            return Optional.empty();
        }

        final Optional<User> owner = itemDao.findUserById(item.get().getOwnerId());
        if (owner.isEmpty()
                || owner.get().getEmail() == null
                || owner.get().getEmail().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(owner.get().getEmail());
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

    private String statusMessageCode(final BookingState status) {
        return switch (status) {
            case BOOKING_CONFIRMED -> "request.status.accepted";
            case BOOKING_REJECTED -> "request.status.declined";
            case BOOKING_PAYMENT_SUBMITTED -> "request.status.paymentSubmitted";
            case BOOKING_PAID -> "request.status.paid";
            default -> "request.status.updated";
        };
    }
}
