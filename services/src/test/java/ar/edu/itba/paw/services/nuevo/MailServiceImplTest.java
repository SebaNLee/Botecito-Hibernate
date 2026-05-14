package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingMailModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingReviewMailModel;
import ar.edu.itba.paw.models.nuevo.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.nuevo.mail.MailRecipientModel;
import ar.edu.itba.paw.models.nuevo.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentReceivedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PublishConfirmationMailModel;
import java.util.Locale;
import java.util.Properties;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
public class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MessageSource messageSource;

    private MailServiceImpl mailService;

    @BeforeEach
    public void setUp() {
        final Properties properties = new Properties();
        properties.setProperty("app.baseUrl", "http://localhost:8080");
        properties.setProperty("mail.reviewRecipient", "review@a.com");

        mailService = new MailServiceImpl(mailSender, templateEngine, messageSource, properties);
    }

    @Test
    public void testResolveEnglishLocaleFromRecipientModel() {
        final Locale result = mailService.resolveLocale(recipient("a@a.com", "A A", PreferredLanguageModel.EN));

        Assertions.assertEquals(Locale.ENGLISH, result);
    }

    @Test
    public void testResolveLocaleDefaultsToSpanish() {
        final MailRecipientModel recipient = recipient("a@a.com", "A A", null);

        final Locale result = mailService.resolveLocale(recipient);

        Assertions.assertEquals(Locale.of("es"), result);
    }

    @Test
    public void testSendPasswordRecoveryEmailUsesModelInput() {
        final PasswordRecoveryMailModel mail = new PasswordRecoveryMailModel();
        mail.setRecipient(recipient("recover@a.com", "Recover User", PreferredLanguageModel.EN));
        mail.setRecoveryToken("token-1");
        stubMessage("mail.passwordRecovery.subject", "Reset your password");
        stubTemplate("password-recovery");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendPasswordRecoveryEmail(mail));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
    }

    @Test
    public void testSendEmailVerificationEmailUsesModelInput() {
        final EmailVerificationMailModel mail = new EmailVerificationMailModel();
        mail.setRecipient(recipient("verify@a.com", "Verify User", PreferredLanguageModel.EN));
        mail.setVerificationToken("token-1");
        stubMessage("mail.emailVerification.subject", "Verify your email");
        stubTemplate("email-verification");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendEmailVerificationEmail(mail));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("email-verification"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/verify-email/token-1",
                contextCaptor.getValue().getVariable("verificationUrl"));
    }

    @Test
    public void testSendPublishConfirmationEmailUsesOwnerModel() {
        final PublishConfirmationMailModel mail = new PublishConfirmationMailModel();
        mail.setOwner(recipient("owner@a.com", "Owner User", PreferredLanguageModel.ES));
        mail.setItemTitle("Boat");
        stubMessage("mail.publishConfirmation.subject", "Published");
        stubTemplate("publish-confirmation");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendPublishConfirmationEmail(mail));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("publish-confirmation"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/my-boats", contextCaptor.getValue().getVariable("profileUrl"));
    }

    @Test
    public void testSendBookingReviewEmailFallsBackToReviewRecipient() {
        final BookingMailModel booking = new BookingMailModel();
        booking.setRequesterName("Requester User");
        booking.setRequesterEmail("requester@a.com");
        booking.setDescription("Please review");

        final BookingReviewMailModel mail = new BookingReviewMailModel();
        mail.setBooking(booking);
        mail.setItemTitle("Boat");
        mail.setLocation("Dock");
        mail.setRequestedDateLabel("2026-05-02");
        mail.setRequestedTimeLabel("10:00 - 12:00");
        stubMessage("mail.requestReview.subject", "Review");
        stubTemplate("booking-review");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendBookingReviewEmail(mail));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("booking-review"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/my-boats#received-booking-requests",
                contextCaptor.getValue().getVariable("profileUrl"));
    }

    @Test
    public void testSendBookingResolutionEmailLinksToBookings() {
        final BookingMailModel booking = new BookingMailModel();
        booking.setRequesterName("Requester User");
        booking.setRequesterEmail("requester@a.com");
        booking.setItemId(10);

        final ar.edu.itba.paw.models.nuevo.mail.BookingResolutionMailModel mail =
                new ar.edu.itba.paw.models.nuevo.mail.BookingResolutionMailModel();
        mail.setRequester(recipient("requester@a.com", "Requester User", PreferredLanguageModel.EN));
        mail.setBooking(booking);
        mail.setItemTitle("Boat");
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn("Updated");
        stubTemplate("booking-resolution");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendBookingResolutionEmail(mail));

        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("booking-resolution"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/bookings#sent-booking-requests",
                contextCaptor.getValue().getVariable("bookingUrl"));
    }

    @Test
    public void testSendPaymentReceivedEmailUsesRequesterModel() {
        final PaymentReceivedMailModel mail = new PaymentReceivedMailModel();
        mail.setRequester(recipient("requester@a.com", "Requester User", PreferredLanguageModel.EN));
        mail.setItemTitle("Boat");
        stubMessage("mail.paymentReceived.subject", "Payment received");
        stubTemplate("payment-received");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendPaymentReceivedEmail(mail));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("payment-received"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/bookings#sent-booking-requests",
                contextCaptor.getValue().getVariable("profileUrl"));
    }

    private void stubMessage(final String code, final String message) {
        Mockito.when(messageSource.getMessage(Mockito.eq(code), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn(message);
    }

    private void stubTemplate(final String template) {
        Mockito.when(templateEngine.process(Mockito.eq(template), Mockito.any()))
                .thenReturn("<p>ok</p>");
    }

    private void stubMimeMessage() {
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
    }

    private static MailRecipientModel recipient(
            final String email, final String displayName, final PreferredLanguageModel preferredLanguage) {
        final MailRecipientModel recipient = new MailRecipientModel();
        recipient.setEmail(email);
        recipient.setDisplayName(displayName);
        recipient.setPreferredLanguage(preferredLanguage);
        return recipient;
    }
}
