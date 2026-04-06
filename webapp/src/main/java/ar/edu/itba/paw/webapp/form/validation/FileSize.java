package ar.edu.itba.paw.webapp.form.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
public @interface FileSize {
    String message() default "{file.upload.sizeError}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    long max() default 5242880; // 5MB
}
