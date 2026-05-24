package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.DetailDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetailImpl implements DetailService {

    private final DetailDao detailDao;
    private final BookingService bookingService;

    @Override
    @Transactional(readOnly = true)
    public Item getItemDetail(final int itemId, final int reviewPage) {
        return requireItemDetail(itemId, reviewPage);
    }

    @Override
    @Transactional(readOnly = true)
    public Item getItemDetail(final int itemId, final int reviewPage, final int hostId) {
        final Item item = requireItemDetail(itemId, reviewPage);
        if (item.getHost() == null || item.getHost().getId() != hostId) {
            throw new ForbiddenOperationException();
        }
        return item;
    }

    private Item requireItemDetail(final int itemId, final int reviewPage) {
        final Item item = detailDao.getItemDetail(itemId, reviewPage).orElseThrow(ItemNotFoundException::new);
        if (item.getLatestVersion() == null) {
            throw new ItemNotFoundException();
        }

        // Fetch transient
        item.setBookings(bookingService.getUpcomingBookings(item));

        return item;
    }
}
