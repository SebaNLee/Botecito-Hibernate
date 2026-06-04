package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.ReportEmail;
import ar.edu.itba.paw.models.entity.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class MailServiceImplTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String EMAIL = "user@example.com";
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String GUEST_EMAIL = "guest@example.com";
    private static final String TOKEN = "abc123token";
    private static final String ITEM_TITLE = "Test Boat";

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MessageSource messageSource;

    private MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        final Properties props = new Properties();
        props.setProperty("app.baseUrl", BASE_URL);
        mailService = new MailServiceImpl(mailSender, templateEngine, messageSource, props);
    }

    // ---- helpers ----

    private static Users user(
            final String email, final String firstName, final String lastName,
            final String language, final String mailToken) {
        final Users u = new Users();
        u.setId(1);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setLanguage(language);
        u.setMailToken(mailToken);
        u.setVerified(true);
        u.setAdmin(false);
        return u;
    }

    private static Item item(final Users host) {
        return Item.builder().id(1).host(host).status(ItemStatusEnum.ACTIVE).build();
    }

    private static Version version(final String title, final Item item) {
        return Version.builder().id(1).title(title).item(item).timezone("UTC").build();
    }

    private static Booking booking(final Version version, final Users guest) {
        return Booking.builder()
                .id(1)
                .version(version)
                .guest(guest)
                .start(LocalDateTime.of(2026, 6, 10, 10, 0))
                .end(LocalDateTime.of(2026, 6, 10, 12, 0))
                .msg("Test message")
                .status(BookingStatusEnum.CONFIRMED)
                .build();
    }

    // ---- tests ----

    @Test
    void sendPublishConfirmationEmail_success() {
        final String[] template = new String[1];
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            template[0] = inv.getArgument(0);
            return "<html></html>";
        });
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("subject");

        mailService.sendPublishConfirmationEmail(
                version(ITEM_TITLE, item(user(OWNER_EMAIL, "Owner", "Last", "es", null))));

        assertEquals("publish-confirmation", template[0]);
    }

    @Test
    void sendPublishConfirmationEmail_nullVersion() {
        mailService.sendPublishConfirmationEmail(null);
    }

    @Test
    void sendFollowerPublishNotificationEmail_success() {
        final String[] template = new String[1];
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            template[0] = inv.getArgument(0);
            return "<html></html>";
        });
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("subject");

        mailService.sendFollowerPublishNotificationEmail(
                user("sub@example.com", "Sub", "Scriber", "es", null),
                version(ITEM_TITLE, item(user(OWNER_EMAIL, "Owner", "Last", "es", null))));

        assertEquals("follower-publish-notification", template[0]);
    }

    @Test
    void sendPasswordRecoveryEmail_success() {
        final String[] template = new String[1];
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            template[0] = inv.getArgument(0);
            return "<html></html>";
        });
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("subject");

        mailService.sendPasswordRecoveryEmail(user(EMAIL, "John", "Doe", "es", TOKEN));

        assertEquals("password-recovery", template[0]);
    }

    @Test
    void sendPasswordRecoveryEmail_blankToken() {
        mailService.sendPasswordRecoveryEmail(user(EMAIL, "John", "Doe", "es", null));
    }

    @Test
    void sendBookingFinishedMail_success() {
        final List<String> templates = new ArrayList<>();
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            templates.add(inv.getArgument(0));
            return "<html></html>";
        });
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("subject");

        mailService.sendBookingFinishedMail(
                booking(version(ITEM_TITLE, item(user(OWNER_EMAIL, "Owner", "Last", "es", null))),
                        user(GUEST_EMAIL, "Guest", "Last", "en", null)));

        assertEquals(2, templates.size());
        assertEquals("booking-lifecycle", templates.get(0));
        assertEquals("booking-lifecycle", templates.get(1));
    }

    @Test
    void sendPreBookingMail_nullBooking() {
        mailService.sendPreBookingMail(null);
    }

    @Test
    void sendReportDismissedEmail_success() {
        final String[] template = new String[1];
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            template[0] = inv.getArgument(0);
            return "<html></html>";
        });
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("subject");

        mailService.sendReportDismissedEmail(ReportEmail.builder()
                .email(EMAIL)
                .displayName("John Doe")
                .locale(Locale.of("es"))
                .itemTitle(ITEM_TITLE)
                .description("Report description")
                .reason(ReportEnum.FAKE)
                .build());

        assertEquals("report-resolution", template[0]);
    }

    @Test
    void sendReportResolutionEmail_nullReason() {
        mailService.sendPublicationRemovedEmail(ReportEmail.builder()
                .email(EMAIL)
                .displayName("John Doe")
                .locale(Locale.of("es"))
                .itemTitle(ITEM_TITLE)
                .description("Report description")
                .reason(null)
                .build());
    }
}
