package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.SubscriptionDao;
import java.util.List;
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

    @Test
    void testListSubscriptionsReturnsPageModel() {
        final Users user = new Users();
        when(subscriptionDao.countSubscriptions(1)).thenReturn(7);
        when(subscriptionDao.listSubscriptions(1, 2, 3)).thenReturn(List.of(user));

        final PageModel<Users> result = subscriptionService.listSubscriptions(1, 2, 3);

        assertEquals(2, result.getPage());
        assertEquals(3, result.getPageSize());
        assertEquals(7, result.getTotalItems());
        assertEquals(3, result.getTotalPages());
        assertEquals(1, result.getContent().size());
    }
}
