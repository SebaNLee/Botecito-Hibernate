package ar.edu.itba.paw.webapp.form.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class ImageGalleryUploadValidator implements ConstraintValidator<ImageGalleryUpload, List<MultipartFile>> {

    private long maxBytesPerFile;
    private int maxFiles;
    private Set<String> allowedContentTypes;

    @Override
    public void initialize(final ImageGalleryUpload annotation) {
        this.maxBytesPerFile = annotation.maxBytesPerFile();
        this.maxFiles = annotation.maxFiles();
        this.allowedContentTypes = new HashSet<>(Arrays.asList(annotation.allowedContentTypes()));
    }

    @Override
    public boolean isValid(final List<MultipartFile> files, final ConstraintValidatorContext context) {
        if (files == null) {
            return true;
        }
        int nonEmpty = 0;
        for (final MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            nonEmpty++;
            if (file.getSize() > maxBytesPerFile) {
                return invalid(context, "{publish.validation.images.size}");
            }
            final String contentType = file.getContentType();
            if (contentType == null || !allowedContentTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
                return invalid(context, "{publish.validation.images.type}");
            }
        }
        if (nonEmpty > maxFiles) {
            return invalid(context, "{publish.validation.images.count}");
        }
        return true;
    }

    private static boolean invalid(final ConstraintValidatorContext context, final String template) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(template).addConstraintViolation();
        return false;
    }
}
