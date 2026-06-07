package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageItemServiceImpl implements ManageItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageItemServiceImpl.class);

    private final ItemService itemService;
    private final BookingService bookingService;
    private final ReportService reportService;

    @Override
    @Transactional
    public void deleteItem(final int itemId, final int ownerId) {
        final Item item = itemService.requireOwnedItem(itemId, ownerId);

        LOGGER.info("User {} deleted item {}", ownerId, itemId);

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
        LOGGER.info("User {} {} item {}", ownerId, enabled ? "enabled" : "disabled", itemId);
    }
}
