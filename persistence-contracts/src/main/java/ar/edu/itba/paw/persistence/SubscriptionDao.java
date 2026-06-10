package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Users;

public interface SubscriptionDao {

    boolean create(int subscriberId, int subscribedToId);

    boolean delete(int subscriberId, int subscribedToId);

    boolean exists(int subscriberId, int subscribedToId);

    PageModel<Users> listSubscriptions(int subscriberId, int page, int pageSize);

    int countSubscriptions(int subscriberId);

    int countFollowers(int userId);

    PageModel<Users> listFollowers(int userId, int page, int pageSize);
}
