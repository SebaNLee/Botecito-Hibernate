package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Users;
import java.util.List;

public interface SubscriptionDao {

    boolean create(int subscriberId, int subscribedToId);

    boolean delete(int subscriberId, int subscribedToId);

    boolean exists(int subscriberId, int subscribedToId);

    List<Users> listSubscriptions(int subscriberId, int page, int pageSize);

    int countSubscriptions(int subscriberId);

    List<Users> listVerifiedSubscribersForPublisher(int publisherId);
}
