package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import java.util.List;

public class ItemImageUtils {
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private ItemImageUtils() {}

    /** Primary image URL or placeholder; {@code servletContextPath} is required when the WAR has a non-root context. */
    public static String resolveImageUrl(
            final ItemService itemService, final int itemId, final String servletContextPath) {
        final String prefix = servletContextPath == null ? "" : servletContextPath;
        final List<Integer> imageIds = itemService.listImageIdsByItemId(itemId);
        if (!imageIds.isEmpty()) {
            return prefix + IMAGE_PATH_PREFIX + imageIds.get(0);
        }
        return prefix + PLACEHOLDER_IMAGE_PATH;
    }
}
