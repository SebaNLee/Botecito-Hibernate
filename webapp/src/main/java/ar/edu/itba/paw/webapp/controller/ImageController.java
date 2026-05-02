package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private static final Duration IMAGE_CACHE_MAX_AGE = Duration.ofHours(1);

    private final ItemService itemService;

    @RequestMapping(value = "/{id:[0-9]+}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> imageById(@PathVariable("id") final int imageId) {
        final Optional<byte[]> imageData = itemService.findImageById(imageId);
        if (imageData.isEmpty() || imageData.get().length == 0) {
            return ResponseEntity.notFound().build();
        }

        final byte[] bytes = imageData.get();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(IMAGE_CACHE_MAX_AGE).cachePublic())
                .contentType(resolveMediaType(bytes))
                .contentLength(bytes.length)
                .body(bytes);
    }

    private static MediaType resolveMediaType(final byte[] imageBytes) {
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
