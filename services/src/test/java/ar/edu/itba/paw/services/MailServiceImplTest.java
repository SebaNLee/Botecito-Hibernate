package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.PreferredLanguage;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

@ExtendWith(MockitoExtension.class)
public class MailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MessageSource messageSource;

    @Mock
    private ItemDao itemDao;

    private MailServiceImpl mailService;

    @BeforeEach
    public void setUp() {
        final Properties properties = new Properties();
        properties.setProperty("app.baseUrl", "http://localhost:8080");
        properties.setProperty("mail.reviewRecipient", "a@a.com");

        mailService = new MailServiceImpl(mailSender, templateEngine, messageSource, itemDao, properties);
    }

    @Test
    public void testResolveEnglish() {
        final User user = new User();
        user.setPreferredLanguage(PreferredLanguage.EN);
        Mockito.when(itemDao.findUserByEmail("a@a.com")).thenReturn(Optional.of(user));
        final Locale result = mailService.resolveLocale("a@a.com");
        Assertions.assertEquals(Locale.ENGLISH, result);
    }

    @Test
    public void testResolveLocaleNoUser() {
        Mockito.when(itemDao.findUserByEmail("b@b.com")).thenReturn(Optional.empty());
        final Locale result = mailService.resolveLocale("b@b.com");
        Assertions.assertEquals(Locale.of("es"), result);
    }

    @Test
    public void testSendBookingReviewEmail() {
        final BookingRequest bookingRequest =
                new BookingRequest("t", 10, "A A", "a@a.com", "es", "a", BookingState.BOOKING_PENDING, Instant.now());
        final javax.mail.internet.MimeMessage mimeMessage = Mockito.mock(javax.mail.internet.MimeMessage.class);

        Mockito.when(itemDao.findUserByEmail("b@b.com")).thenReturn(Optional.empty());
        Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        Mockito.when(messageSource.getMessage(
                        Mockito.eq("mail.requestReview.subject"), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn("a");
        Mockito.when(templateEngine.process(Mockito.eq("booking-review"), Mockito.any()))
                .thenReturn("<p>ok</p>");

        Assertions.assertDoesNotThrow(() -> mailService.sendBookingReviewEmail(
                bookingRequest, "b@b.com", "Boat", "Loc", "2026-05-02", "10:00 - 12:00"));
    }

    @Test
    public void testPasswordRecoveryEmail() {
        final javax.mail.internet.MimeMessage mimeMessage = Mockito.mock(javax.mail.internet.MimeMessage.class);

        Mockito.when(itemDao.findUserByEmail("recover@a.com")).thenReturn(Optional.empty());
        Mockito.when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        Mockito.when(messageSource.getMessage(
                        Mockito.eq("mail.passwordRecovery.subject"), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn("Reset your password");
        Mockito.when(templateEngine.process(Mockito.eq("password-recovery"), Mockito.any()))
                .thenReturn("<p>reset</p>");

        Assertions.assertDoesNotThrow(
                () -> mailService.sendPasswordRecoveryEmail("recover@a.com", "Recover User", "token-1"));
    }
}
