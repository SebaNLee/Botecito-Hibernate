package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.services.ItemService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private static final Duration IMAGE_CACHE_MAX_AGE = Duration.ofHours(1);

    private final ItemService itemInterface;

    @RequestMapping("/{id:[0-9]+}")
    public ResponseEntity<byte[]> imageById(@PathVariable("id") final int imageId) {
        return itemInterface
                .findImageWithDataById(imageId)
                .map(Image::getData)
                .filter(data -> data.length > 0)
                .map(ImageController::okCachedImage)
                .orElse(ResponseEntity.notFound().build());
    }

    private static ResponseEntity<byte[]> okCachedImage(final byte[] bytes) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(IMAGE_CACHE_MAX_AGE).cachePublic())
                .contentType(resolveMediaType(bytes))
                .contentLength(bytes.length)
                .body(bytes);
    }

    private static MediaType resolveMediaType(final byte[] bytes) {
        if (bytes.length > 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
            return MediaType.IMAGE_JPEG;
        }
        if (bytes.length > 3 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
