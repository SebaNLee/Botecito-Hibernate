package ar.edu.itba.paw.webapp.presentation;

import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ImagePresentation {

    private static final Duration IMAGE_CACHE_MAX_AGE = Duration.ofHours(1);
    private static final MediaType IMAGE_WEBP = MediaType.parseMediaType("image/webp");

    public ResponseEntity<byte[]> imageResponse(final byte[] data) {
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(IMAGE_CACHE_MAX_AGE).cachePublic())
                .contentType(resolveMediaType(data))
                .contentLength(data.length)
                .body(data);
    }

    private static MediaType resolveMediaType(final byte[] bytes) {
        if (bytes.length > 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return MediaType.IMAGE_JPEG;
        }
        if (bytes.length > 3 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if (bytes.length > 2 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return MediaType.IMAGE_GIF;
        }
        if (bytes.length > 11
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return IMAGE_WEBP;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
