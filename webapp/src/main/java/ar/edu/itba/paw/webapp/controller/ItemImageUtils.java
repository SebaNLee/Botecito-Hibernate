package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import java.util.List;

public class ItemImageUtils {
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private ItemImageUtils() {}

    public static String resolveImageUrl(final ItemService itemService, final int itemId) {
        final List<Integer> imageIds = itemService.listImageIdsByItemId(itemId);
        if (!imageIds.isEmpty()) {
            return IMAGE_PATH_PREFIX + imageIds.get(0);
        }
        return PLACEHOLDER_IMAGE_PATH;
    }
}
