package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;
import java.util.List;

public interface SubscriptionService {

    boolean subscribe(int subscriberId, int subscribedToId);

    boolean unsubscribe(int subscriberId, int subscribedToId);

    boolean isSubscribed(int subscriberId, int subscribedToId);

    PageModel<Users> listSubscriptions(int subscriberId, int page, int pageSize);

    int countFollowers(int userId);

    List<Users> listVerifiedSubscribersForPublisher(int publisherId);
}
