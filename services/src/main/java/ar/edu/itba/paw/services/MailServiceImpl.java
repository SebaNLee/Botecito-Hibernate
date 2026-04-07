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
    private final ItemDao itemDao;
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
            final ItemDao itemDao,
            final Properties credentialsProperties) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
        this.itemDao = itemDao;
        final String baseUrl = requireProperty(credentialsProperties, "app.baseUrl");
        // TODO replace this config-backed review recipient with data loaded from persistence once the
        // application has a real owner/admin recipient model in the DB.
        // Basicamente es a donde llegan los mails de request, de momento van todos a botecitos deberia ser el que hizo
        // la publicacion
        this.reviewRecipient = requireProperty(credentialsProperties, "mail.reviewRecipient");
        this.actionBaseUrl = baseUrl + "/bookings";
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
    public void sendPublishConfirmationEmail(
            final String recipientEmail,
            final String ownerName,
            final String itemTitle,
            final String ownerDeleteToken) {
        final Locale locale = resolveLocale(recipientEmail);
        final Context context = new Context(locale);
        context.setVariable("ownerName", ownerName);
        context.setVariable("itemTitle", itemTitle);
        context.setVariable("publishUrl", itemBaseUrl.replace("/item", "/publish"));
        context.setVariable("deleteUrl", itemBaseUrl.replace("/item", "/publish/" + ownerDeleteToken + "/delete"));
        sendHtmlEmail(
                recipientEmail,
                getMessage("mail.publishConfirmation.subject", locale, itemTitle),
                templateEngine.process("publish-confirmation", context));
    }

    @Override
    public void sendBookingReviewEmail(final BookingRequest bookingRequest, final String ownerEmail) {
        final String recipientEmail;
        if (ownerEmail != null && !ownerEmail.isBlank()) {
            recipientEmail = ownerEmail;
        } else {
            recipientEmail = resolveBookingReviewRecipient(bookingRequest).orElse(reviewRecipient);
        }
        final Locale locale = resolveLocale(recipientEmail);
        final Context context = new Context(locale);
        context.setVariable("bookingRequest", bookingRequest);
        context.setVariable("acceptUrl", actionBaseUrl + "/" + bookingRequest.getToken() + "/accept");
        context.setVariable("declineUrl", actionBaseUrl + "/" + bookingRequest.getToken() + "/decline");
        sendHtmlEmail(
                recipientEmail,
                getMessage("mail.requestReview.subject", locale, bookingRequest.getRequesterName()),
                templateEngine.process("booking-review", context));
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
    public Locale resolveLocale(final String recipientIdentifier) {
        final Optional<Locale> userLocale = itemDao.findUserByEmail(recipientIdentifier)
                .map(user -> toSupportedLocale(user.getPreferredLanguage()));
        if (userLocale.isPresent()) {
            return userLocale.get();
        }
        return new Locale("es");
    }

    private static Locale toSupportedLocale(final String preferredLanguage) {
        if ("en".equalsIgnoreCase(preferredLanguage)) {
            return Locale.ENGLISH;
        }
        return new Locale("es");
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

    private String statusMessageCode(final BookingState status) {
        return switch (status) {
            case BOOKING_CONFIRMED -> "request.status.accepted";
            case BOOKING_REJECTED -> "request.status.declined";
            default -> "request.status.updated";
        };
    }
}
