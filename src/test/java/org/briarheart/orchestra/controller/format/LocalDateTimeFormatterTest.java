package org.briarheart.orchestra.controller.format;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class LocalDateTimeFormatterTest {
    private LocalDateTimeFormatter formatter = new LocalDateTimeFormatter();

    @Test
    void shouldParseDateTime() throws ParseException {
        String dateTimeString = "2020-01-01T15:30:51.564";
        LocalDateTime parsedDateTime = formatter.parse(dateTimeString, Locale.getDefault());
        assertNotNull(parsedDateTime);
        assertEquals(dateTimeString, parsedDateTime.toString());
    }

    @Test
    void shouldThrowExceptionOnParseWhenTextIsNull() {
        assertThrows(IllegalArgumentException.class, () -> formatter.parse(null, Locale.getDefault()));
    }

    @Test
    void shouldThrowExceptionOnParseWhenTextIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> formatter.parse("", Locale.getDefault()));
    }

    @Test
    void shouldThrowExceptionOnParseWhenDateTimeFormatIsNotValid() {
        assertThrows(ParseException.class, () -> formatter.parse("01.01.2020T15:30:51.564", Locale.getDefault()));
    }

    @Test
    void shouldPrintDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2020, 1, 1, 15, 30, 51, 564);
        assertEquals("2020-01-01T15:30:51.000000564", formatter.print(dateTime, Locale.getDefault()));
    }

    @Test
    void shouldThrowExceptionOnPrintWhenDateTimeObjectIsNull() {
        assertThrows(IllegalArgumentException.class, () -> formatter.print(null, Locale.getDefault()));
    }
}
