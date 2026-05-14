package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.PublishBoatForm;
import ar.edu.itba.paw.webapp.form.PublishBoatForm.UploadedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Component
public class PublishImagePresentation {

    public static final int MAX_GALLERY_IMAGES = 3;

    public ModelAndView publishImagesUpload(final PublishBoatForm form, final BindingResult errors) {
        appendAndClear(form, errors);
        return new ModelAndView("redirect:/publish");
    }

    public ModelAndView publishImagesRemove(final PublishBoatForm form, final int index) {
        form.removeUploadedImageAt(index);
        return new ModelAndView("redirect:/publish");
    }

    public ModelAndView publishImagesReorder(final PublishBoatForm form, final String order) {
        final List<Integer> parsed = parseOrderCsv(order);
        form.reorderUploadedImages(parsed);
        return new ModelAndView("redirect:/publish");
    }

    public ResponseEntity<byte[]> publishPreviewImage(final PublishBoatForm form, final int index) {
        final UploadedImage image = form.getUploadedImageAt(index);
        if (image == null || image.getSize() == 0) {
            return ResponseEntity.notFound().build();
        }
        final byte[] imageBytes = image.getData();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(resolvePreviewMediaType(image.getContentType()))
                .contentLength(imageBytes.length)
                .body(imageBytes);
    }

    public void appendAndClear(final PublishBoatForm form, final BindingResult errors) {
        if (!errors.hasFieldErrors("files")) {
            appendUploadedImagesIfPresent(form, errors);
        }
        form.setFiles(new ArrayList<>());
    }

    public List<String> buildUploadedImagePreviewUrls(final PublishBoatForm form) {
        final List<String> urls = new ArrayList<>();
        for (int i = 0; i < form.getUploadedImageCount(); i++) {
            urls.add("/publish/preview-image" + "/" + i);
        }
        return urls;
    }

    private static void appendUploadedImagesIfPresent(final PublishBoatForm form, final BindingResult errors) {
        final List<MultipartFile> uploaded = form.getFiles();
        if (uploaded == null || uploaded.isEmpty()) {
            return;
        }
        for (final MultipartFile file : uploaded) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (form.getUploadedImageCount() >= MAX_GALLERY_IMAGES) {
                errors.rejectValue("files", "publish.validation.images.count");
                return;
            }
            final String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType) || !contentType.regionMatches(true, 0, "image/", 0, 6)) {
                continue;
            }
            try {
                final byte[] imageBytes = file.getBytes();
                if (imageBytes.length > 0) {
                    form.appendUploadedImage(imageBytes, contentType);
                }
            } catch (final IOException ignored) {
            }
        }
    }

    private static List<Integer> parseOrderCsv(final String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        final List<Integer> result = new ArrayList<>();
        for (final String token : Arrays.asList(csv.split(","))) {
            final String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (final NumberFormatException ignored) {
                return List.of();
            }
        }
        return result;
    }

    private static MediaType resolvePreviewMediaType(final String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (final IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
