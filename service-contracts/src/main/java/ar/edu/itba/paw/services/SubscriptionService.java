package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Users;
import java.util.List;

public interface SubscriptionService {

    boolean subscribe(int subscriberId, int subscribedToId);

    boolean unsubscribe(int subscriberId, int subscribedToId);

    boolean isSubscribed(int subscriberId, int subscribedToId);

    List<Users> listSubscriptions(int subscriberId);

    List<Users> listVerifiedSubscribersForPublisher(int publisherId);
}
