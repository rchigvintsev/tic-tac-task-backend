package org.briarheart.orchestra.model.validation.constraints;

import org.briarheart.orchestra.model.validation.validator.NotPastLocalDateTimeValidator;

import javax.validation.ClockProvider;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.Validator;
import java.lang.annotation.*;

/**
 * The annotated element must be a class that contains date and/or time fields representing today's date/time or
 * date/time in future. Target class field names must be specified in properties {@link #dateFieldName()} (for date)
 * and {@link #timeFieldName()} (for time). At least one field name must be specified. If both field names are
 * specified they will be validated in conjunction. There is a key difference between this constraint and
 * {@link javax.validation.constraints.Future} in validation of dates: today's date is considered valid by this
 * constraint.
 * <p>
 * <i>Now</i> is defined by the {@link ClockProvider} attached to the {@link Validator}.
 <p>
 * Supported types are:
 * <ul>
 *     <li>{@code java.time.LocalDate}</li>
 *     <li>{@code java.time.LocalTime}</li>
 * </ul>
 * <p>
 * {@code null} values are considered valid.
 *
 * @author Roman Chigvintsev
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = NotPastLocalDateTimeValidator.class)
public @interface NotPast {
    String message() default "{org.briarheart.orchestra.model.validation.constraints.NotPast.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String dateFieldName() default "";

    String timeFieldName() default "";
}
