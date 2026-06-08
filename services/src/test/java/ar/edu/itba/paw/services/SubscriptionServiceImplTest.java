package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.SubscriptionDao;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceImplTest {

    private static final int USER_ID = 1;
    private static final int OTHER_USER_ID = 2;

    @Mock
    private SubscriptionDao subscriptionDao;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Test
    public void testSubscribe() {
        when(subscriptionDao.create(USER_ID, OTHER_USER_ID)).thenReturn(true);

        assertTrue(subscriptionService.subscribe(USER_ID, OTHER_USER_ID));
    }

    @Test
    public void testSubscribeAlreadySubscribed() {
        when(subscriptionDao.create(USER_ID, OTHER_USER_ID)).thenReturn(false);

        assertFalse(subscriptionService.subscribe(USER_ID, OTHER_USER_ID));
    }

    @Test
    public void testSubscribeSelf() {
        assertFalse(subscriptionService.subscribe(USER_ID, USER_ID));
    }

    @Test
    public void testUnsubscribe() {
        when(subscriptionDao.delete(USER_ID, OTHER_USER_ID)).thenReturn(true);

        assertTrue(subscriptionService.unsubscribe(USER_ID, OTHER_USER_ID));
    }

    @Test
    public void testUnsubscribeNotSubscribed() {
        when(subscriptionDao.delete(USER_ID, OTHER_USER_ID)).thenReturn(false);

        assertFalse(subscriptionService.unsubscribe(USER_ID, OTHER_USER_ID));
    }

    @Test
    public void testUnsubscribeSelf() {
        assertFalse(subscriptionService.unsubscribe(USER_ID, USER_ID));
    }

    @Test
    public void testIsSubscribed() {
        when(subscriptionDao.exists(USER_ID, OTHER_USER_ID)).thenReturn(true);

        assertTrue(subscriptionService.isSubscribed(USER_ID, OTHER_USER_ID));
    }

    @Test
    public void testListSubscriptions() {
        when(subscriptionDao.countSubscriptions(USER_ID)).thenReturn(1);
        when(subscriptionDao.listSubscriptions(USER_ID, 1, 10)).thenReturn(List.of(new Users()));

        var result = subscriptionService.listSubscriptions(USER_ID, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalItems());
    }

    @Test
    public void testCountFollowers() {
        when(subscriptionDao.countFollowers(USER_ID)).thenReturn(3);

        assertEquals(3, subscriptionService.countFollowers(USER_ID));
    }

    @Test
    public void testListVerifiedSubscribersForPublisher() {
        when(subscriptionDao.listVerifiedSubscribersForPublisher(USER_ID)).thenReturn(List.of(new Users()));

        var result = subscriptionService.listVerifiedSubscribersForPublisher(USER_ID);

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
