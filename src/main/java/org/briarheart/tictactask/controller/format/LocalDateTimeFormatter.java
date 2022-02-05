package org.briarheart.tictactask.controller.format;

import org.briarheart.tictactask.util.DateTimeUtils;
import org.springframework.format.Formatter;

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
        try {
            return DateTimeUtils.parseIsoDateTime(text);
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DateTimeParseException) {
                throw new ParseException(e.getMessage(), ((DateTimeParseException) cause).getErrorIndex());
            }
            throw e;
        }
    }

    @Override
    public String print(LocalDateTime object, Locale locale) {
        return DateTimeUtils.formatIsoDateTime(object);
    }
}
