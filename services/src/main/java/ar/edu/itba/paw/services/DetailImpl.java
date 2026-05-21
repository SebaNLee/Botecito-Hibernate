package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.persistence.DetailDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetailImpl implements DetailService {

    private final DetailDao detailDao;

    @Override
    @Transactional(readOnly = true)
    public Item getItemDetail(final int itemId, final int reviewPage) {
        final Item item = detailDao.getItemDetail(itemId, reviewPage).orElseThrow(ItemNotFoundException::new);
        if (item.getLatestVersion() == null) {
            throw new ItemNotFoundException();
        }
        return item;
    }
}
