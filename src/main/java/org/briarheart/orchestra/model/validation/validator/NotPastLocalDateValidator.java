package org.briarheart.orchestra.model.validation.validator;

import org.briarheart.orchestra.model.validation.constraints.NotPast;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator;

import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Constraint validator for {@link NotPast} annotation.
 *
 * @author Roman Chigvintsev
 * @see NotPast
 */
public class NotPastLocalDateValidator implements HibernateConstraintValidator<NotPast, LocalDate> {
    /**
     * Checks whether the given value represents today's date or date in the future.
     *
     * @param value value to check
     * @param context validation context
     * @return {@code true} if the given value is {@code null} or a valid date
     */
    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        ZonedDateTime startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime startOfDayUtc = startOfDay.withZoneSameInstant(ZoneOffset.UTC);
        return !value.isBefore(startOfDayUtc.toLocalDate());
    }
}
