package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.UserService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class GalleryController {

    private static final long MAX_BYTES_PER_FILE = 5_242_880L;
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ItemService itemService;
    private final UserService userService;

    @RequestMapping(value = "/item/{id}/gallery", method = RequestMethod.GET)
    public ModelAndView gallery(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "error", required = false) final String error,
            final HttpServletRequest request) {
        final Item item = ownedItemOrNull(itemId);
        if (item == null) {
            return notOwnerView(itemId);
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

    @RequestMapping(value = "/item/{id}/gallery/upload", method = RequestMethod.POST)
    public ModelAndView upload(
            @PathVariable("id") final int itemId,
            @RequestParam(value = "files", required = false) final List<MultipartFile> files) {
        final Item item = ownedItemOrNull(itemId);
        if (item == null) {
            return notOwnerView(itemId);
        }

        if (files == null || files.isEmpty()) {
            return new ModelAndView("redirect:/item/" + itemId + "/gallery");
        }

        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_BYTES_PER_FILE) {
                return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=size");
            }
            final String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=type");
            }
            try {
                itemService.appendImage(itemId, file.getBytes());
            } catch (final IllegalArgumentException ex) {
                return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=count");
            } catch (final IOException ex) {
                return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=read");
            }
        }
        return new ModelAndView("redirect:/item/" + itemId + "/gallery");
    }

    @RequestMapping(value = "/item/{id}/gallery/delete", method = RequestMethod.POST)
    public ModelAndView deleteImage(@PathVariable("id") final int itemId, @RequestParam("imageId") final int imageId) {
        final Item item = ownedItemOrNull(itemId);
        if (item == null) {
            return notOwnerView(itemId);
        }
        itemService.deleteImageFromItem(itemId, imageId);
        return new ModelAndView("redirect:/item/" + itemId + "/gallery");
    }

    @RequestMapping(value = "/item/{id}/gallery/reorder", method = RequestMethod.POST)
    public ModelAndView reorder(
            @PathVariable("id") final int itemId, @RequestParam(value = "order", required = false) final String order) {
        final Item item = ownedItemOrNull(itemId);
        if (item == null) {
            return notOwnerView(itemId);
        }
        final List<Integer> parsed = parseOrder(order);
        if (parsed.isEmpty()) {
            return new ModelAndView("redirect:/item/" + itemId + "/gallery");
        }
        try {
            itemService.reorderImagesForItem(itemId, parsed);
        } catch (final IllegalArgumentException ex) {
            return new ModelAndView("redirect:/item/" + itemId + "/gallery?error=reorder");
        }
        return new ModelAndView("redirect:/item/" + itemId + "/gallery");
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

    private ModelAndView notOwnerView(final int itemId) {
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

    private static List<Integer> parseOrder(final String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        final List<Integer> result = new ArrayList<>();
        for (final String token : csv.split(",")) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (final NumberFormatException ex) {
                return List.of();
            }
        }
        return result;
    }
}
