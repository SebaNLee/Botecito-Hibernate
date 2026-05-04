package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.controller.support.ImageMvcSupport;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ItemService itemService;

    @RequestMapping(value = "/{id:[0-9]+}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> imageById(@PathVariable("id") final int imageId) {
        final Optional<byte[]> imageData = itemService.findImageById(imageId);
        if (imageData.isEmpty() || imageData.get().length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ImageMvcSupport.okCachedImage(imageData.get());
    }
}
