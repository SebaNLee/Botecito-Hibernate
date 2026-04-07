package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import java.util.List;

public class ItemImageUtils {
    public static String resolveImageUrl(ItemService itemService, int itemId) {
        List<Integer> imageIds = itemService.listImageIdsByItemId(itemId);
        if (!imageIds.isEmpty()) {
            return "/image/" + imageIds.get(0);
        }
        return ""; // or path to a default image
    }
}
