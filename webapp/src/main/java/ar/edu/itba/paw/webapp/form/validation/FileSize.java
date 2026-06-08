package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.util.UploadLimits;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME) // TODO we do not want image local persist in the future
@Constraint(validatedBy = FileSizeValidator.class)
public @interface FileSize {
    String message() default "{file.upload.sizeError}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long max() default UploadLimits.MAX_FILE_BYTES;
}
