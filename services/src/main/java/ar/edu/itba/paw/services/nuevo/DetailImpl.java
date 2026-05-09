package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.persistence.nuevo.DetailDao;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class DetailImpl implements DetailInterface {
    private final DetailDao detailDao;

    @Override
    public Optional<ItemModel> getItemById(int itemId) {
        return detailDao.getItemById(itemId);
    }
}
