package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.models.nuevo.MyBoatsItem;
import ar.edu.itba.paw.services.nuevo.ItemInterface;

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

    private final ItemInterface itemInterface;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ModelAndView galleryPage(final int itemId, final String error, final HttpServletRequest request) {
        final UsersOrm currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> ownedItem = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (ownedItem.isEmpty()) {
            return notOwnerView();
        }

        final List<Integer> imageIds = itemInterface.listImageIds(itemId);
        final List<String> imageUrls = new ArrayList<>(imageIds.size());
        final String contextPath = request.getContextPath();
        for (final Integer imageId : imageIds) {
            imageUrls.add(contextPath + "/image/" + imageId);
        }

        final ModelAndView mav = new ModelAndView("item-gallery");
        mav.addObject("item", ownedItem.get());
        mav.addObject("imageIds", imageIds);
        mav.addObject("imageUrls", imageUrls);
        mav.addObject("imageCount", imageIds.size());
        mav.addObject("maxImages", MAX_GALLERY_IMAGES);
        if (error != null) {
            mav.addObject("galleryError", error);
        }
        return mav;
    }

    public ModelAndView uploadGallery(final int itemId, final List<MultipartFile> files) {
        final UsersOrm currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> ownedItem = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (ownedItem.isEmpty()) {
            return notOwnerView();
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
            final Optional<Integer> newId = itemInterface.uploadGalleryImage(itemId, currentUser.getId(), bytes);
            if (newId.isEmpty()) {
                return redirectToGallery(itemId, "count");
            }
        }
        return redirectToGallery(itemId);
    }

    public ModelAndView deleteGalleryImage(final int itemId, final int imageId) {
        final UsersOrm currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> ownedItem = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (ownedItem.isEmpty()) {
            return notOwnerView();
        }

        itemInterface.deleteImageFromGallery(imageId);
        return redirectToGallery(itemId);
    }

    public ModelAndView reorderGallery(final int itemId, final String order) {
        final UsersOrm currentUser = authenticatedUserResolver.currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final Optional<MyBoatsItem> ownedItem = itemInterface.findMyBoatsItemByIdForOwner(itemId, currentUser.getId());
        if (ownedItem.isEmpty()) {
            return notOwnerView();
        }

        final List<Integer> parsed = parseGalleryImageOrderCsv(order);
        if (parsed.isEmpty()) {
            return redirectToGallery(itemId);
        }

        if (!itemInterface.reorderGallery(itemId, currentUser.getId(), parsed)) {
            return redirectToGallery(itemId, "reorder");
        }

        return redirectToGallery(itemId);
    }

    private static ModelAndView notOwnerView() {
        return new ModelAndView("redirect:/403");
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
