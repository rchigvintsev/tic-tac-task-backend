package org.briarheart.tictactask.util;

import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility class that provides methods to parse and format date/time.
 *
 * @author Roman Chigvintsev
 */
public class DateTimeUtils {
    private DateTimeUtils() {
        //no instance
    }

    public static LocalDate parseIsoDate(String s) {
        Assert.hasLength(s, "String to parse must not be null or empty");
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Failed to parse date from string '" + s + "': " + e.getMessage(), e);
        }
    }

    public static LocalDateTime parseIsoDateTime(String s) {
        Assert.hasLength(s, "String to parse must not be null or empty");
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Failed to parse date/time from string '" + s + "': " + e.getMessage(), e);
        }
    }

    public static String formatIsoDate(LocalDate date) {
        Assert.notNull(date, "Date to format must not be null");
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static String formatIsoDateTime(LocalDateTime dateTime) {
        Assert.notNull(dateTime, "Date/time to format must not be null");
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
