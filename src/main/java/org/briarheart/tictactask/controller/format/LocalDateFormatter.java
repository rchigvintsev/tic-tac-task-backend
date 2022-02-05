package org.briarheart.tictactask.controller.format;

import org.briarheart.tictactask.util.DateTimeUtils;
import org.springframework.format.Formatter;

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
        try {
            return DateTimeUtils.parseIsoDate(text);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DateTimeParseException) {
                throw new ParseException(e.getMessage(), ((DateTimeParseException) cause).getErrorIndex());
            }
            throw e;
        }
    }

    @Override
    public String print(LocalDate object, Locale locale) {
        return DateTimeUtils.formatIsoDate(object);
    }
}
