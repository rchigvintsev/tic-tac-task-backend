package org.briarheart.orchestra.model.validation.validator;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.validation.UtcClockProvider;
import org.briarheart.orchestra.model.validation.constraints.NotPast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintValidatorContext;
import java.time.*;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class NotPastLocalDateTimeValidatorTest {
    private ConstraintValidatorContext context;
    private LocalDate today;
    private NotPastLocalDateTimeValidator validator;

    @BeforeEach
    void setUp() {
        context = Mockito.mock(ConstraintValidatorContext.class);
        Mockito.when(context.getClockProvider()).thenReturn(new UtcClockProvider());

        ZonedDateTime startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        today = startOfDay.withZoneSameInstant(ZoneOffset.UTC).toLocalDate();

        validator = new NotPastLocalDateTimeValidator();
    }

    @Test
    void shouldThrowExceptionWhenTargetObjectIsNull() {
        assertThrows(IllegalArgumentException.class, () -> validator.isValid(null, context),
                "Target object must not be null");
    }

    @Test
    void shouldThrowExceptionWhenRequiredAnnotationPropertiesAreNotSpecified() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.isValid(new RequiredAnnotationPropertiesMissing(), context),
                "At least one of the following annotation properties must be set: 'dateFieldName' or 'timeFieldName'"
        );
    }

    @Test
    void shouldThrowExceptionWhenSpecifiedDateFieldIsNotFound() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.isValid(new DateMissing(null), context),
                "Field 'date' is not found in class " + DateMissing.class.getName()
        );
    }

    @Test
    void shouldThrowExceptionWhenSpecifiedTimeFieldIsNotFound() {
        assertThrows(
                IllegalArgumentException.class,
                () -> validator.isValid(new TimeMissing(null), context),
                "Field 'time' is not found in class " + TimeMissing.class.getName()
        );
    }

    @Test
    void shouldReturnTrueWhenDateAndTimeAreNull() {
        assertTrue(validator.isValid(new DateAndTime(null, null), context));
    }

    @Test
    void shouldReturnTrueWhenDateIsNull() {
        assertTrue(validator.isValid(new OnlyDate(null), context));
    }

    @Test
    void shouldReturnTrueWhenTimeIsNull() {
        assertTrue(validator.isValid(new OnlyTime(null), context));
    }

    @Test
    void shouldReturnFalseForYesterday() {
        assertFalse(validator.isValid(new OnlyDate(today.minusDays(1)), context));
    }

    @Test
    void shouldReturnTrueForToday() {
        assertTrue(validator.isValid(new OnlyDate(today), context));
    }

    @Test
    void shouldReturnTrueForTomorrow() {
        assertTrue(validator.isValid(new OnlyDate(today.plusDays(1)), context));
    }

    @Test
    void shouldReturnFalseForCurrentTimeMinusOneMinute() {
        OnlyTime target = new OnlyTime(currentTime().minus(1, ChronoUnit.MINUTES));
        assertFalse(validator.isValid(target, context));
    }

    @Test
    void shouldReturnTrueForCurrentTimePlusOneMinute() {
        OnlyTime target = new OnlyTime(currentTime().plus(1, ChronoUnit.MINUTES));
        assertTrue(validator.isValid(target, context));
    }

    @Test
    void shouldBeTolerateToDifferenceInSeconds() {
        OnlyTime target = new OnlyTime(currentTime().minus(1, ChronoUnit.SECONDS));
        assertTrue(validator.isValid(target, context));
    }

    @Test
    void shouldReturnFalseForYesterdayAtCurrentTime() {
        assertFalse(validator.isValid(new DateAndTime(today.minusDays(1), currentTime()), context));
    }

    @Test
    void shouldReturnFalseForYesterdayAtCurrentTimePlusOneMinute() {
        DateAndTime target = new DateAndTime(today.minusDays(1), currentTime().plus(1, ChronoUnit.MINUTES));
        assertFalse(validator.isValid(target, context));
    }

    @Test
    void shouldReturnTrueForTodayAtCurrentTime() {
        assertTrue(validator.isValid(new DateAndTime(today, currentTime()), context));
    }

    @Test
    void shouldReturnTrueForTomorrowAtCurrentTime() {
        assertTrue(validator.isValid(new DateAndTime(today.plusDays(1), currentTime()), context));
    }

    private LocalTime currentTime() {
        return LocalTime.now(ZoneOffset.UTC);
    }

    @NotPast(dateFieldName = "date", timeFieldName = "time")
    @RequiredArgsConstructor
    private static class DateAndTime {
        private final LocalDate date;
        private final LocalTime time;
    }

    @NotPast(dateFieldName = "date")
    @RequiredArgsConstructor
    private static class OnlyDate {
        private final LocalDate date;
    }

    @NotPast(timeFieldName = "time")
    @RequiredArgsConstructor
    private static class OnlyTime {
        private final LocalTime time;
    }

    @NotPast(dateFieldName = "date", timeFieldName = "time")
    @RequiredArgsConstructor
    private static class DateMissing {
        private final LocalTime time;
    }

    @NotPast(dateFieldName = "date", timeFieldName = "time")
    @RequiredArgsConstructor
    private static class TimeMissing {
        private final LocalDate date;
    }

    @NotPast
    private static class RequiredAnnotationPropertiesMissing {
    }
}
