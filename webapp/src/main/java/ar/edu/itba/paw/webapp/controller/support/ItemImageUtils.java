package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.services.ItemService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemImageUtils {
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    private ItemImageUtils() {}

    /** Primary image URL or placeholder; {@code servletContextPath} is required when the WAR has a non-root context. */
    public static String resolveImageUrl(
            final ItemService itemService, final int itemId, final String servletContextPath) {
        final String prefix = servletContextPath == null ? "" : servletContextPath;
        final Optional<Integer> coverId = itemService.findCoverImageIdByItemId(itemId);
        if (coverId.isPresent()) {
            return prefix + IMAGE_PATH_PREFIX + coverId.get();
        }
        return prefix + PLACEHOLDER_IMAGE_PATH;
    }

    /** All gallery image URLs in display order; falls back to a single placeholder when the item has no images. */
    public static List<String> resolveImageUrls(
            final ItemService itemService, final int itemId, final String servletContextPath) {
        final String prefix = servletContextPath == null ? "" : servletContextPath;
        final List<Integer> ids = itemService.listImageIdsByItemIdOrdered(itemId);
        final List<String> urls = new ArrayList<>(Math.max(ids.size(), 1));
        for (final Integer id : ids) {
            urls.add(prefix + IMAGE_PATH_PREFIX + id);
        }
        if (urls.isEmpty()) {
            urls.add(prefix + PLACEHOLDER_IMAGE_PATH);
        }
        return urls;
    }
}
