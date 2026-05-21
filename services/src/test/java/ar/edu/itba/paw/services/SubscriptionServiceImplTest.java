package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.paw.persistence.SubscriptionDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    @Mock
    private SubscriptionDao subscriptionDao;

    @Test
    void testSubscribeRejectsSelfSubscription() {
        final boolean result = subscriptionService.subscribe(7, 7);

        assertFalse(result);
        verify(subscriptionDao, never()).create(7, 7);
    }

    @Test
    void testSubscribeCreatesMissingSubscription() {
        when(subscriptionDao.exists(1, 2)).thenReturn(false);

        final boolean result = subscriptionService.subscribe(1, 2);

        assertTrue(result);
        verify(subscriptionDao).create(1, 2);
    }

    @Test
    void testSubscribeIsIdempotent() {
        when(subscriptionDao.exists(1, 2)).thenReturn(true);

        final boolean result = subscriptionService.subscribe(1, 2);

        assertTrue(result);
        verify(subscriptionDao, never()).create(1, 2);
    }

    @Test
    void testUnsubscribeRejectsSelfSubscription() {
        final boolean result = subscriptionService.unsubscribe(7, 7);

        assertFalse(result);
        verify(subscriptionDao, never()).delete(7, 7);
    }
}
