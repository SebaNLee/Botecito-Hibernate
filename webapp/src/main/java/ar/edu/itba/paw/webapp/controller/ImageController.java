package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.presentation.ImagePresentation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ImageController {

    private final ItemService itemService;
    private final ImagePresentation imagePresentation;

    @GetMapping("/image/{id:[1-9]\\d*}")
    public ResponseEntity<byte[]> imageById(@PathVariable("id") final int imageId) {
        return itemService
                .findImageById(imageId)
                .map(Image::getData)
                .map(imagePresentation::imageResponse)
                .orElse(ResponseEntity.notFound().build());
    }
}
