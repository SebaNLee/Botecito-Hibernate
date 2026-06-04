package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private TemplateEngine templateEngine;
    @Mock
    private MessageSource messageSource;

    private MailServiceImpl mailService;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.setProperty("app.baseUrl", "http://localhost:8080/xd");
        mailService = new MailServiceImpl(mailSender, templateEngine, messageSource, props);
    }

    @Test
    void preBookingMailCorrectTemplate() {
        defaultMocks();
        String[] template = new String[1];
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            template[0] = inv.getArgument(0);
            return "<html><body><p>Alo!</p></body></html>";
        });

        mailService.sendPreBookingMail(
                booking(version("Boat", item(user("botecito.dev@gmail.com", "O", "W", "es", null))),
                        user("botecito.user@gmail.com", "G", "S", "en", null)));

        assertEquals("booking-lifecycle", template[0]);
    }

    @Test
    void bookingFinishedMailSendsTwoEmails() {
        defaultMocks();
        AtomicInteger count = new AtomicInteger(0);
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            count.incrementAndGet();
            return "<html><body><p>Alo!</p></body></html>";
        });

        mailService.sendBookingFinishedMail(
                booking(version("Boat", item(user("botecito.dev@gmail.com", "O", "W", "es", null))),
                        user("botecito.user@gmail.com", "G", "S", "en", null)));

        assertEquals(2, count.get());
    }

    @Test
    void passwordRecoverySkipsWhenTokenIsBlank() {
        AtomicInteger count = new AtomicInteger(0);
        lenient().when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(inv -> {
            count.incrementAndGet();
            return "<html><body><p>Alo!</p></body></html>";
        });

        mailService.sendPasswordRecoveryEmail(user("botecito.user@gmail.com", "J", "D", "es", null));

        assertEquals(0, count.get());
    }

    @Test
    void resolveLocaleReturnsCorrectLocale() {
        assertEquals(Locale.of("es"), MailServiceImpl.resolveLocale(user("botecito.user@gmail.com", "A", "B", "es", null)));
        assertEquals(Locale.ENGLISH, MailServiceImpl.resolveLocale(user("botecito.user@gmail.com", "A", "B", "en", null)));
        assertEquals(Locale.of("es"), MailServiceImpl.resolveLocale(null));
    }

    private static Users user(String email, String firstName, String lastName, String language, String mailToken) {
        Users u = new Users();
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setLanguage(language);
        u.setMailToken(mailToken);
        return u;
    }

    private static Item item(Users host) {
        return Item.builder().id(1).host(host).status(ItemStatusEnum.ACTIVE).build();
    }

    private static Version version(String title, Item item) {
        return Version.builder().id(1).title(title).item(item).timezone("UTC").build();
    }

    private static Booking booking(Version version, Users guest) {
        return Booking.builder()
                .id(1)
                .version(version)
                .guest(guest)
                .start(LocalDateTime.of(2026, 6, 10, 10, 0))
                .end(LocalDateTime.of(2026, 6, 10, 12, 0))
                .msg("Test message")
                .status(BookingStatusEnum.PENDING)
                .build();
    }

    private void defaultMocks() {
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("subject");
    }
}
