package org.briarheart.orchestra.controller.format;

import org.springframework.format.Formatter;
import org.springframework.util.Assert;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Formatter for {@link LocalDateTime} type that uses {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} pattern to parse
 * and print date/time.
 *
 * @author Roman Chigvintsev
 */
public class LocalDateTimeFormatter implements Formatter<LocalDateTime> {
    @Override
    public LocalDateTime parse(String text, Locale locale) throws ParseException {
        Assert.hasLength(text, "Text to parse must not be null or empty");
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ParseException("Failed to parse date/time from string '" + text + "': " + e.getMessage(),
                    e.getErrorIndex());
        }
    }

    @Override
    public String print(LocalDateTime object, Locale locale) {
        Assert.notNull(object, "Date/time to print must not be null");
        return object.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
