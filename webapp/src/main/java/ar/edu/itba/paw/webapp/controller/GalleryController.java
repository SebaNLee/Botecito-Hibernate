package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.controller.support.GalleryMvcSupport;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryMvcSupport galleryMvcSupport;

    @RequestMapping(value = "/item/{id}/gallery", method = RequestMethod.GET)
    public ModelAndView gallery(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "error", required = false) final String error,
            final HttpServletRequest request) {
        return galleryMvcSupport.galleryPage(itemId, error, request);
    }

    @RequestMapping(value = "/item/{id}/gallery/upload", method = RequestMethod.POST)
    public ModelAndView upload(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "files", required = false) final List<MultipartFile> files) {
        return galleryMvcSupport.uploadGallery(itemId, files);
    }

    @RequestMapping(value = "/item/{id}/gallery/delete", method = RequestMethod.POST)
    public ModelAndView deleteImage(@PathVariable("id") final int itemId, @RequestParam("imageId") final int imageId) {
        return galleryMvcSupport.deleteGalleryImage(itemId, imageId);
    }

    @RequestMapping(value = "/item/{id}/gallery/reorder", method = RequestMethod.POST)
    public ModelAndView reorder(
            @PathVariable("id") final int itemId, @RequestParam(value = "order", required = false) final String order) {
        return galleryMvcSupport.reorderGallery(itemId, order);
    }
}
