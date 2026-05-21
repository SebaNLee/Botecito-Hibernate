package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.PaymentProof;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import java.time.LocalDateTime;
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
        mailService = new MailServiceImpl(mailSender, templateEngine, messageSource, properties);
    }

    @Test
    public void testResolveEnglishLocaleFromRecipientModel() {
        final Locale result = mailService.resolveLocale(user("a@a.com", "A", "A", "en"));

        Assertions.assertEquals(Locale.ENGLISH, result);
    }

    @Test
    public void testResolveLocaleDefaultsToSpanish() {
        final Users user = user("a@a.com", "A", "A", null);

        final Locale result = mailService.resolveLocale(user);

        Assertions.assertEquals(Locale.of("es"), result);
    }

    @Test
    public void testSendPasswordRecoveryEmailUsesModelInput() {
        final Users user = user("recover@a.com", "Recover", "User", "en");
        user.setMailToken("token-1");
        stubMessage("mail.passwordRecovery.subject", "Reset your password");
        stubTemplate("password-recovery");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendPasswordRecoveryEmail(user));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
    }

    @Test
    public void testSendEmailVerificationEmailUsesModelInput() {
        final Users user = user("verify@a.com", "Verify", "User", "en");
        user.setMailToken("token-1");
        stubMessage("mail.emailVerification.subject", "Verify your email");
        stubTemplate("email-verification");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendEmailVerificationEmail(user));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("email-verification"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/verify-email/token-1",
                contextCaptor.getValue().getVariable("verificationUrl"));
    }

    @Test
    public void testSendPublishConfirmationEmailUsesOwnerModel() {
        final Version version = version("Boat", user("owner@a.com", "Owner", "User", "es"));
        version.getItem().setId(44);
        stubMessage("mail.publishConfirmation.subject", "Published");
        stubTemplate("publish-confirmation");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendPublishConfirmationEmail(version));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("publish-confirmation"), contextCaptor.capture());
        Assertions.assertEquals(
                "http://localhost:8080/my-boats", contextCaptor.getValue().getVariable("profileUrl"));
    }

    @Test
    public void testSendFollowerPublishNotificationEmailUsesSubscriberAndVersionModel() {
        final Users subscriber = user("subscriber@a.com", "Sub", "User", "en");
        final Version version = version("Boat", user("owner@a.com", "Owner", "User", "es"));
        version.getItem().setId(44);
        stubMessage("mail.followerPublish.subject", "New publication");
        stubTemplate("follower-publish-notification");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendFollowerPublishNotificationEmail(subscriber, version));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("follower-publish-notification"), contextCaptor.capture());
        Assertions.assertEquals("Sub User", contextCaptor.getValue().getVariable("recipientName"));
        Assertions.assertEquals("Owner User", contextCaptor.getValue().getVariable("ownerName"));
        Assertions.assertEquals("Boat", contextCaptor.getValue().getVariable("itemTitle"));
        Assertions.assertEquals(
                "http://localhost:8080/item/44", contextCaptor.getValue().getVariable("itemUrl"));
    }

    @Test
    public void testSendPreBookingMailUsesHostAndBookingModel() {
        final Booking booking = booking();
        stubBookingMessages("mail.booking.preBooking");
        stubTemplate("booking-lifecycle");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendPreBookingMail(booking));

        Mockito.verify(mailSender).send(Mockito.any(MimeMessage.class));
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("booking-lifecycle"), contextCaptor.capture());
        Assertions.assertEquals("Host User", contextCaptor.getValue().getVariable("recipientName"));
        Assertions.assertEquals("Sail Boat", contextCaptor.getValue().getVariable("itemTitle"));
        Assertions.assertEquals(
                "http://localhost:8080/requests/incoming",
                contextCaptor.getValue().getVariable("actionUrl"));
    }

    @Test
    public void testSendRefusedPaymentMailUsesPaymentProofReason() {
        final Booking booking = booking();
        final PaymentProof proof = new PaymentProof();
        proof.setReplyMsg("Payment sent");
        proof.setRefuseMsg("Unreadable proof");
        booking.setPaymentProof(proof);
        stubBookingMessages("mail.booking.paymentRefused");
        stubTemplate("booking-lifecycle");
        stubMimeMessage();

        Assertions.assertDoesNotThrow(() -> mailService.sendRefusedPaymentMail(booking));

        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        Mockito.verify(templateEngine).process(Mockito.eq("booking-lifecycle"), contextCaptor.capture());
        Assertions.assertEquals("Unreadable proof", contextCaptor.getValue().getVariable("refusalReason"));
    }

    private void stubMessage(final String code, final String message) {
        Mockito.when(messageSource.getMessage(Mockito.eq(code), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn(message);
    }

    private void stubTemplate(final String template) {
        Mockito.when(templateEngine.process(Mockito.eq(template), Mockito.any()))
                .thenReturn("<p>ok</p>");
    }

    private void stubBookingMessages(final String prefix) {
        stubMessage(prefix + ".subject", "Booking subject");
        stubMessage(prefix + ".heading", "Booking heading");
        stubMessage(prefix + ".body", "Booking body");
        stubMessage(prefix + ".note", "Booking note");
        stubMessage(prefix + ".action", "Booking action");
    }

    private void stubMimeMessage() {
        Mockito.when(mailSender.createMimeMessage()).thenReturn(Mockito.mock(MimeMessage.class));
    }

    private static Booking booking() {
        final Users host = user("host@a.com", "Host", "User", "en");
        final Users guest = user("guest@a.com", "Guest", "User", "en");
        final Version version = version("Sail Boat", host);
        version.setTimezone("UTC");
        final Booking booking = new Booking();
        booking.setId(12);
        booking.setGuest(guest);
        booking.setVersion(version);
        booking.setStart(LocalDateTime.of(2026, 5, 20, 15, 0));
        booking.setEnd(LocalDateTime.of(2026, 5, 20, 17, 0));
        booking.setMsg("Can we start on time?");
        return booking;
    }

    private static Version version(final String title, final Users host) {
        final Item item = new Item();
        item.setHost(host);
        final Version version = new Version();
        version.setItem(item);
        version.setTitle(title);
        version.setTimezone("UTC");
        return version;
    }

    private static Users user(
            final String email, final String firstName, final String lastName, final String language) {
        final Users user = new Users();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLanguage(language);
        return user;
    }
}
