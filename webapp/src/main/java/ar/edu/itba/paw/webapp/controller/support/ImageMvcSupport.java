package ar.edu.itba.paw.webapp.controller.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Builds image {@link ResponseEntity} payloads with cache and content-type
 * detection. Used by {@link ar.edu.itba.paw.webapp.controller.ImageController}.
 */
public final class ImageMvcSupport {

    private static final Duration IMAGE_CACHE_MAX_AGE = Duration.ofHours(1);

    private ImageMvcSupport() {}

    public static ResponseEntity<byte[]> okCachedImage(final byte[] bytes) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(IMAGE_CACHE_MAX_AGE).cachePublic())
                .contentType(resolveMediaType(bytes))
                .contentLength(bytes.length)
                .body(bytes);
    }

    public static MediaType resolveMediaType(final byte[] imageBytes) {
        try {
            final String mediaType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(imageBytes));
            if (mediaType != null) {
                return MediaType.parseMediaType(mediaType);
            }
        } catch (final IOException ignored) {
            // Fall back to a generic binary content type if detection fails.
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
