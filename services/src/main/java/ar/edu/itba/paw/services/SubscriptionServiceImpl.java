package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.SubscriptionDao;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionDao subscriptionDao;

    @Override
    @Transactional
    public boolean subscribe(final int subscriberId, final int subscribedToId) {
        if (subscriberId == subscribedToId) {
            return false;
        }
        if (subscriptionDao.exists(subscriberId, subscribedToId)) {
            return true;
        }
        subscriptionDao.create(subscriberId, subscribedToId);
        return true;
    }

    @Override
    @Transactional
    public boolean unsubscribe(final int subscriberId, final int subscribedToId) {
        if (subscriberId == subscribedToId) {
            return false;
        }
        subscriptionDao.delete(subscriberId, subscribedToId);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscribed(final int subscriberId, final int subscribedToId) {
        return subscriberId != subscribedToId && subscriptionDao.exists(subscriberId, subscribedToId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Users> listSubscriptions(final int subscriberId) {
        return subscriptionDao.listSubscriptions(subscriberId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Users> listVerifiedSubscribersForPublisher(final int publisherId) {
        return subscriptionDao.listVerifiedSubscribersForPublisher(publisherId);
    }
}
