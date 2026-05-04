package ar.edu.itba.paw.webapp.controller.support;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.GalleryImageUpload;
import ar.edu.itba.paw.services.GalleryOwnerUploadResult;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

/** Gallery pages: auth, model assembly, multipart reads, and redirects. */
@Component
@RequiredArgsConstructor
public final class GalleryMvcSupport {

    private final ItemService itemService;
    private final UserService userService;

    public ModelAndView galleryPage(final int itemId, final String error, final HttpServletRequest request) {
        final Item item = ownedItemOrNull(itemId);
        if (item == null) {
            return notOwnerView();
        }

        final List<Integer> imageIds = itemService.listImageIdsByItemIdOrdered(itemId);
        final List<String> imageUrls = new ArrayList<>(imageIds.size());
        final String contextPath = request.getContextPath();
        for (final Integer imageId : imageIds) {
            imageUrls.add(contextPath + "/image/" + imageId);
        }

        final ModelAndView mav = new ModelAndView("item-gallery");
        mav.addObject("item", item);
        mav.addObject("imageIds", imageIds);
        mav.addObject("imageUrls", imageUrls);
        mav.addObject("imageCount", itemService.countImagesByItemId(itemId));
        mav.addObject("maxImages", itemService.maxImagesPerItem());
        if (error != null) {
            mav.addObject("galleryError", error);
        }
        return mav;
    }

    public ModelAndView uploadGallery(final int itemId, final List<MultipartFile> files) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return notOwnerView();
        }
        if (files == null || files.isEmpty()) {
            return redirectToGallery(itemId);
        }

        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            final byte[] bytes = readMultipartBytes(file).orElse(null);
            if (bytes == null) {
                return redirectToGallery(itemId, "read");
            }
            final GalleryOwnerUploadResult result = itemService.uploadGalleryImage(
                    itemId,
                    currentUser.getId(),
                    new GalleryImageUpload(file.getOriginalFilename(), file.getContentType(), bytes));
            final ModelAndView redirect = galleryUploadRedirect(itemId, result);
            if (redirect != null) {
                return redirect;
            }
        }
        return redirectToGallery(itemId);
    }

    public ModelAndView deleteGalleryImage(final int itemId, final int imageId) {
        if (ownedItemOrNull(itemId) == null) {
            return notOwnerView();
        }
        itemService.deleteImageFromItem(itemId, imageId);
        return redirectToGallery(itemId);
    }

    public ModelAndView reorderGallery(final int itemId, final String order) {
        if (ownedItemOrNull(itemId) == null) {
            return notOwnerView();
        }
        final List<Integer> parsed = itemService.parseGalleryImageOrderCsv(order);
        if (parsed.isEmpty()) {
            return redirectToGallery(itemId);
        }
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return notOwnerView();
        }
        if (!itemService.reorderGalleryForOwner(itemId, currentUser.getId(), parsed)) {
            return redirectToGallery(itemId, "reorder");
        }
        return redirectToGallery(itemId);
    }

    private ModelAndView galleryUploadRedirect(final int itemId, final GalleryOwnerUploadResult result) {
        return switch (result.getStatus()) {
            case SUCCESS -> null;
            case NOT_OWNER -> notOwnerView();
            case EMPTY_FILE -> null;
            case INVALID_CONTENT_TYPE -> redirectToGallery(itemId, "type");
            case FILE_TOO_LARGE -> redirectToGallery(itemId, "size");
            case GALLERY_FULL -> redirectToGallery(itemId, "count");
        };
    }

    private Item ownedItemOrNull(final int itemId) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return null;
        }
        final Item item = itemService.findItemById(itemId).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return null;
        }
        return item;
    }

    private ModelAndView notOwnerView() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new ModelAndView("redirect:/login");
        }
        return new ModelAndView("redirect:/403");
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }

    private static ModelAndView redirectToGallery(final int itemId) {
        return new ModelAndView("redirect:/item/" + itemId + "/gallery");
    }

    private static ModelAndView redirectToGallery(final int itemId, final String errorQueryValue) {
        return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=" + errorQueryValue);
    }

    private static Optional<byte[]> readMultipartBytes(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(file.getBytes());
        } catch (final IOException ex) {
            return Optional.empty();
        }
    }
}
