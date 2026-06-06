package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
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
        return subscriptionDao.create(subscriberId, subscribedToId);
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
    public PageModel<Users> listSubscriptions(final int subscriberId, final int page, final int pageSize) {
        final int totalItems = subscriptionDao.countSubscriptions(subscriberId);
        return new PageModel<>(
                subscriptionDao.listSubscriptions(subscriberId, page, pageSize), page, pageSize, totalItems);
    }

    @Override
    @Transactional(readOnly = true)
    public int countFollowers(final int userId) {
        return subscriptionDao.countFollowers(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Users> listVerifiedSubscribersForPublisher(final int publisherId) {
        return subscriptionDao.listVerifiedSubscribersForPublisher(publisherId);
    }
}
