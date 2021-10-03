package org.briarheart.tictactask.controller.format;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class LocalDateFormatterTest {
    private final LocalDateFormatter formatter = new LocalDateFormatter();

    @Test
    void shouldParseDate() throws ParseException {
        String dateString = "2020-01-01";
        LocalDate parsedDate = formatter.parse(dateString, Locale.getDefault());
        assertNotNull(parsedDate);
        assertEquals(dateString, parsedDate.toString());
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
    void shouldThrowExceptionOnParseWhenDateFormatIsNotValid() {
        assertThrows(ParseException.class, () -> formatter.parse("01.01.2020", Locale.getDefault()));
    }

    @Test
    void shouldPrintDate() {
        assertEquals("2020-01-01", formatter.print(LocalDate.of(2020, 1, 1), Locale.getDefault()));
    }

    @Test
    void shouldThrowExceptionOnPrintWhenDateObjectIsNull() {
        assertThrows(IllegalArgumentException.class, () -> formatter.print(null, Locale.getDefault()));
    }
}
