package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.Version;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EditGalleryImageSeed {
    private final int id;
    private final String url;

    public static List<EditGalleryImageSeed> fromVersion(final Version version, final String contextPath) {
        if (version.getMedia() == null || version.getMedia().isEmpty()) {
            return List.of();
        }
        final List<EditGalleryImageSeed> images = new ArrayList<>();
        version.getMedia().stream()
                .sorted(Comparator.comparingInt(m -> m.getId().getIndex()))
                .forEach(media -> {
                    if (media.getImage() == null || media.getImage().getId() == null) {
                        return;
                    }
                    images.add(new EditGalleryImageSeed(
                            media.getImage().getId(),
                            contextPath + "/image/" + media.getImage().getId()));
                });
        return images;
    }
}
