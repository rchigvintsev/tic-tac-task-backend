package org.briarheart.tictactask.controller.format;

import org.springframework.format.Formatter;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Formatter for {@link LocalDate} type that uses {@link DateTimeFormatter#ISO_LOCAL_DATE} pattern to parse and
 * print date.
 *
 * @author Roman Chigvintsev
 */
public class LocalDateFormatter implements Formatter<LocalDate> {
    @Override
    public LocalDate parse(String text, Locale locale) throws ParseException {
        Assert.hasLength(text, "Text to parse must not be null or empty");
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new ParseException("Failed to parse date from string '" + text + "': " + e.getMessage(),
                    e.getErrorIndex());
        }
    }

    @Override
    public String print(LocalDate object, Locale locale) {
        Assert.notNull(object, "Date to print must not be null");
        return object.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
