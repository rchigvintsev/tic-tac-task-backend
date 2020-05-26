package org.briarheart.orchestra.model.validation.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roman Chigvintsev
 */
class NotPastLocalDateValidatorTest {
    private NotPastLocalDateValidator validator;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        validator = new NotPastLocalDateValidator();
        ZonedDateTime startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault());
        today = startOfDay.withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
    }

    @Test
    void shouldReturnTrueForNullValue() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldReturnFalseForYesterday() {
        assertFalse(validator.isValid(today.minusDays(1), null));
    }

    @Test
    void shouldReturnTrueForToday() {
        assertTrue(validator.isValid(today, null));
    }

    @Test
    void shouldReturnTrueForTomorrow() {
        assertTrue(validator.isValid(today.plusDays(1), null));
    }
}
