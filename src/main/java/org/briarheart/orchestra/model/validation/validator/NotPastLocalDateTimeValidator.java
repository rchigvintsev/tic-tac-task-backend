package org.briarheart.orchestra.model.validation.validator;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.orchestra.model.validation.constraints.NotPast;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidator;
import org.springframework.util.ReflectionUtils;

import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.*;

/**
 * Constraint validator for {@link NotPast} annotation.
 *
 * @author Roman Chigvintsev
 * @see NotPast
 */
public class NotPastLocalDateTimeValidator implements HibernateConstraintValidator<NotPast, Object> {
    /**
     * Checks whether the specified properties of the given target object represent today's date/time or date/time
     * in the future.
     *
     * @param target object containing properties to check
     * @param context validation context
     * @return {@code true} if the specified properties contain {@code null}s or a valid date/time
     */
    @Override
    public boolean isValid(Object target, ConstraintValidatorContext context) {
        Assert.notNull(target, "Target object must not be null");

        NotPast notPastAnnotation = target.getClass().getAnnotation(NotPast.class);

        String dateFieldName = notPastAnnotation.dateFieldName();
        String timeFieldName = notPastAnnotation.timeFieldName();
        if (dateFieldName.isEmpty() && timeFieldName.isEmpty()) {
            throw new IllegalArgumentException("At least one of the following annotation properties must be set: "
                    + "'dateFieldName' or 'timeFieldName'");
        }

        LocalDate date = null;
        if (!dateFieldName.isEmpty()) {
            date = getFieldValue(target, dateFieldName, LocalDate.class);
        }

        LocalTime time = null;
        if (!timeFieldName.isEmpty()) {
            time = getFieldValue(target, timeFieldName, LocalTime.class);
        }

        if (date == null && time == null) {
            return true;
        }

        Clock clock = context.getClockProvider().getClock();

        LocalTime currentTime = getCurrentTime(clock);
        if (date == null) {
            return !time.isBefore(currentTime);
        }
        LocalDate currentDate = getCurrentDate(clock);
        if (time == null) {
            return !date.isBefore(currentDate);
        }
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        return !dateTime.isBefore(LocalDateTime.of(currentDate, currentTime));
    }

    private LocalDate getCurrentDate(Clock clock) {
        ZonedDateTime startOfDayAtSystemZone = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime startOfDayAtClockZone = startOfDayAtSystemZone.withZoneSameInstant(clock.getZone());
        return startOfDayAtClockZone.toLocalDate();
    }

    private LocalTime getCurrentTime(Clock clock) {
        return LocalTime.now(clock).withSecond(0).withNano(0);
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object target, String fieldName, Class<T> fieldType) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName, fieldType);
        if (field == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is not found in class "
                    + target.getClass().getName());
        }
        field.setAccessible(true);
        return (T) ReflectionUtils.getField(field, target);
    }
}
