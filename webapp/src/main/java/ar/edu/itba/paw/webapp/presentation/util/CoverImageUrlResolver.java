package ar.edu.itba.paw.webapp.presentation.util;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Version;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CoverImageUrlResolver {

    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";

    public Map<Integer, String> resolve(final List<Item> items, final HttpServletRequest request) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final Map<Integer, String> imageUrlsByItemId = new LinkedHashMap<>();
        for (final Item item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            imageUrlsByItemId.put(item.getId(), coverImageUrl(item.getLatestVersion(), contextPath));
        }
        return imageUrlsByItemId;
    }

    private static String coverImageUrl(final Version version, final String contextPath) {
        if (version == null || version.getMedia() == null || version.getMedia().isEmpty()) {
            return contextPath + PLACEHOLDER_IMAGE_PATH;
        }
        return version.getMedia().stream()
                .filter(m -> m.getImage() != null && m.getId() != null)
                .min(Comparator.comparingInt(m -> m.getId().getIndex()))
                .map(Media::getImage)
                .map(image -> contextPath + IMAGE_PATH_PREFIX + image.getId())
                .orElse(contextPath + PLACEHOLDER_IMAGE_PATH);
    }
}
