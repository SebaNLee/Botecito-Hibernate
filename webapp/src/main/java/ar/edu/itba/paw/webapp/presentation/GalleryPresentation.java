package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.MyBoatsItem;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class GalleryPresentation {

    private static final int MAX_GALLERY_IMAGES = 5;

    private final ItemService itemInterface;

    public ModelAndView galleryPage(
            final BotecitoUserDetails principal,
            final int itemId,
            final String error,
            final HttpServletRequest request) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        final MyBoatsItem ownedItem = itemInterface.requireOwnedItem(itemId, principal.getId());
        final List<Integer> imageIds = itemInterface.listImageIds(itemId);
        final List<String> imageUrls = new ArrayList<>(imageIds.size());
        final String contextPath = request.getContextPath();
        for (final Integer imageId : imageIds) {
            imageUrls.add(contextPath + "/image/" + imageId);
        }

        final ModelAndView mav = new ModelAndView("item-gallery");
        mav.addObject("item", ownedItem);
        mav.addObject("imageIds", imageIds);
        mav.addObject("imageUrls", imageUrls);
        mav.addObject("imageCount", imageIds.size());
        mav.addObject("maxImages", MAX_GALLERY_IMAGES);
        if (error != null) {
            mav.addObject("galleryError", error);
        }
        return mav;
    }

    public ModelAndView uploadGallery(
            final BotecitoUserDetails principal, final int itemId, final List<MultipartFile> files) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        if (files == null || files.isEmpty()) {
            return redirectToGallery(itemId);
        }

        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            final byte[] bytes = readMultipartBytes(file);
            if (bytes == null) {
                return redirectToGallery(itemId, "read");
            }
            final Optional<Integer> newId = itemInterface.uploadGalleryImage(itemId, principal.getId(), bytes);
            if (newId.isEmpty()) {
                return redirectToGallery(itemId, "count");
            }
        }
        return redirectToGallery(itemId);
    }

    public ModelAndView deleteGalleryImage(final BotecitoUserDetails principal, final int itemId, final int imageId) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        itemInterface.deleteImageFromGallery(itemId, imageId, principal.getId());
        return redirectToGallery(itemId);
    }

    public ModelAndView reorderGallery(final BotecitoUserDetails principal, final int itemId, final String order) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }

        final List<Integer> parsed = parseGalleryImageOrderCsv(order);
        if (parsed.isEmpty()) {
            return redirectToGallery(itemId);
        }

        if (!itemInterface.reorderGallery(itemId, principal.getId(), parsed)) {
            return redirectToGallery(itemId, "reorder");
        }

        return redirectToGallery(itemId);
    }

    private static ModelAndView redirectToGallery(final int itemId) {
        return new ModelAndView("redirect:/item/" + itemId + "/gallery");
    }

    private static ModelAndView redirectToGallery(final int itemId, final String errorQueryValue) {
        return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=" + errorQueryValue);
    }

    private static byte[] readMultipartBytes(final MultipartFile file) {
        try {
            return file.getBytes();
        } catch (final java.io.IOException ex) {
            return null;
        }
    }

    static List<Integer> parseGalleryImageOrderCsv(final String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        final List<Integer> ids = new ArrayList<>();
        final StringTokenizer tokenizer = new StringTokenizer(csv, ",");
        while (tokenizer.hasMoreTokens()) {
            final String token = tokenizer.nextToken().trim();
            try {
                ids.add(Integer.parseInt(token));
            } catch (final NumberFormatException e) {
                return List.of();
            }
        }
        return ids;
    }
}
