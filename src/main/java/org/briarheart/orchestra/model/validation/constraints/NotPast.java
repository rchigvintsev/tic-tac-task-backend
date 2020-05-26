package org.briarheart.orchestra.model.validation.constraints;

import org.briarheart.orchestra.model.validation.validator.NotPastLocalDateValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * The annotated element must be a {@link java.time.LocalDate} that represents today's date or date in the future
 * with time-zone offset for UTC.
 *
 * @author Roman Chigvintsev
 */
@Target({
        ElementType.METHOD,
        ElementType.FIELD,
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.PARAMETER,
        ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NotPastLocalDateValidator.class)
public @interface NotPast {
    String message() default "{org.briarheart.orchestra.model.validation.constraints.NotPast.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
