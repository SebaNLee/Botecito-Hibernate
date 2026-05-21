package ar.edu.itba.paw.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.PublishDao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishServiceImplTest {

    @InjectMocks
    private PublishServiceImpl publishService;

    @Mock
    private PublishDao publishDao;

    @Mock
    private MailService mailService;

    @Mock
    private SubscriptionService subscriptionService;

    @Test
    void testCreateNotifiesOwnerAndVerifiedSubscribers() {
        final Version version = version(22, 5, "Kayak");
        final Users subscriber = user(9, "subscriber@example.com", true);
        when(publishDao.create(
                        5,
                        1,
                        "Kayak",
                        "Nice",
                        100,
                        2,
                        BigDecimal.valueOf(200),
                        1,
                        3,
                        "America/Argentina/Buenos_Aires",
                        "ACTIVE",
                        List.of(),
                        List.of()))
                .thenReturn(22);
        when(publishDao.findById(22)).thenReturn(Optional.of(version));
        when(subscriptionService.listVerifiedSubscribersForPublisher(5)).thenReturn(List.of(subscriber));

        publishService.create(5, 1, "Kayak", "Nice", 100, 2, BigDecimal.valueOf(200), 1, 3, List.of(), List.of());

        verify(mailService).sendPublishConfirmationEmail(version);
        verify(mailService).sendFollowerPublishNotificationEmail(subscriber, version);
    }

    private static Version version(final int itemId, final int ownerId, final String title) {
        final Item item = new Item();
        item.setId(itemId);
        item.setHost(user(ownerId, "owner@example.com", true));
        final Version version = new Version();
        version.setItem(item);
        version.setTitle(title);
        return version;
    }

    private static Users user(final int id, final String email, final boolean verified) {
        final Users user = new Users();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setLanguage("EN");
        user.setVerified(verified);
        return user;
    }
}
