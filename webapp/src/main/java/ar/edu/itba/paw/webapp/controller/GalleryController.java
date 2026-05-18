package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.presentation.GalleryPresentation;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final GalleryPresentation galleryPresentation;

    @RequestMapping(value = "/item/{id}/gallery", method = RequestMethod.GET)
    public ModelAndView gallery(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "error", required = false) final String error,
            final HttpServletRequest request) {
        return galleryPresentation.galleryPage(user, itemId, error, request);
    }

    @RequestMapping(value = "/item/{id}/gallery/upload", method = RequestMethod.POST)
    public ModelAndView upload(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "files", required = false) final List<MultipartFile> files) {
        return galleryPresentation.uploadGallery(user, itemId, files);
    }

    @RequestMapping(value = "/item/{id}/gallery/delete", method = RequestMethod.POST)
    public ModelAndView deleteImage(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam("imageId") final int imageId) {
        return galleryPresentation.deleteGalleryImage(user, itemId, imageId);
    }

    @RequestMapping(value = "/item/{id}/gallery/reorder", method = RequestMethod.POST)
    public ModelAndView reorder(
            @AuthenticationPrincipal final BotecitoUserDetails user,
            @PathVariable("id") final int itemId,
            @RequestParam(value = "order", required = false) final String order) {
        return galleryPresentation.reorderGallery(user, itemId, order);
    }
}
