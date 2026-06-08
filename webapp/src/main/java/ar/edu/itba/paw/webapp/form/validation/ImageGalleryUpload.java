package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.util.UploadLimits;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageGalleryUploadValidator.class)
public @interface ImageGalleryUpload {

    String message() default "{publish.validation.images.invalid}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long maxBytesPerFile() default UploadLimits.MAX_FILE_BYTES;

    int maxFiles() default 10;

    String[] allowedContentTypes() default {"image/jpeg", "image/png", "image/webp", "image/gif"};
}
