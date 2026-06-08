package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.HostReviewsPage;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ItemService itemService;
    private final ReviewService reviewService;

    @Override
    @Transactional(readOnly = true)
    public PageModel<Item> listProfileListings(
            final int ownerId, final int page, final int pageSize, final String sortBy) {
        final PageModel<Item> results = itemService.listOwnerItems(ownerId, null, null, page, pageSize, sortBy);
        reviewService.attachReviewSummaries(results.getContent());
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public HostReviewsPage findHostReviewsPage(final int hostUserId, final int page) {
        return reviewService.findHostReviewsPage(hostUserId, page);
    }
}
