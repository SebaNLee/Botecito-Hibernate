package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.persistence.SubscriptionDao;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionDao subscriptionDao;

    @Override
    @Transactional
    public boolean subscribe(final int subscriberId, final int subscribedToId) {
        if (subscriberId == subscribedToId) {
            return false;
        }
        final boolean created = subscriptionDao.create(subscriberId, subscribedToId);
        if (created) {
            LOGGER.info("User {} subscribed to user {}", subscriberId, subscribedToId);
        }
        return created;
    }

    @Override
    @Transactional
    public boolean unsubscribe(final int subscriberId, final int subscribedToId) {
        if (subscriberId == subscribedToId) {
            return false;
        }
        final boolean deleted = subscriptionDao.delete(subscriberId, subscribedToId);
        if (deleted) {
            LOGGER.info("User {} unsubscribed from user {}", subscriberId, subscribedToId);
        }
        return deleted;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscribed(final int subscriberId, final int subscribedToId) {
        return subscriberId != subscribedToId && subscriptionDao.exists(subscriberId, subscribedToId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageModel<Users> listSubscriptions(final int subscriberId, final int page, final int pageSize) {
        final long totalItems = subscriptionDao.countSubscriptions(subscriberId);
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
    public PageModel<Users> listVerifiedSubscribersForPublisher(
            final int publisherId, final int page, final int pageSize) {
        return subscriptionDao.listVerifiedSubscribersForPublisher(publisherId, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public int countVerifiedFollowers(final int publisherId) {
        return subscriptionDao.countVerifiedFollowers(publisherId);
    }
}
