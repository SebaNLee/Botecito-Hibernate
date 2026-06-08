package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.HostReviewsPage;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;

public interface ProfileService {

    PageModel<Item> listProfileListings(int ownerId, int page, int pageSize, String sortBy);

    HostReviewsPage findHostReviewsPage(int hostUserId, int page);
}
