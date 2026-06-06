package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageItemServiceImpl implements ManageItemService {

    private final ItemService itemService;
    private final BookingService bookingService;
    private final ReportService reportService;

    @Override
    @Transactional
    public void deleteItem(final int itemId, final int ownerId) {
        final Item item = itemService.requireOwnedItem(itemId, ownerId);
        bookingService.deleteAllSelfBlocks(item);
        reportService.deleteAllByItemId(itemId);

        boolean isSoft = itemService.getVersionCount(itemId) > 1 || bookingService.itemHasBookings(itemId);
        itemService.deleteItem(item, isSoft);
    }

    @Override
    @Transactional
    public void setEnabled(final int itemId, final int ownerId, final boolean enabled) {
        final Item item = itemService.requireOwnedItem(itemId, ownerId);
        item.setStatus(enabled ? ItemStatusEnum.ACTIVE : ItemStatusEnum.INACTIVE);
    }
}
