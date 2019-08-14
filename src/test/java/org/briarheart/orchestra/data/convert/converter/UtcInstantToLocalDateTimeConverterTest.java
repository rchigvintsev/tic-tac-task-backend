package org.briarheart.orchestra.data.convert.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Roman Chigvintsev
 */
class UtcInstantToLocalDateTimeConverterTest {
    private UtcInstantToLocalDateTimeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UtcInstantToLocalDateTimeConverter();
    }

    @Test
    void shouldConvertMoscowTimeInstantToLocalDateTimeUsingUtcTimeZone() {
        LocalDateTime dateTime = LocalDateTime.of(2019, Month.AUGUST, 14, 11, 0);
        Instant moscowTimeInstant = dateTime.atZone(ZoneId.of("Europe/Moscow")).toInstant();
        LocalDateTime utcDateTime = converter.convert(moscowTimeInstant);
        assertNotNull(utcDateTime);
        assertEquals(8, utcDateTime.getHour());
    }
}