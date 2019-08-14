package org.briarheart.orchestra.data.convert.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * This class converts {@link Instant} to {@link LocalDateTime} using UTC zone offset.
 *
 * @author Roman Chigvintsev
 */
@ReadingConverter
public class UtcInstantToLocalDateTimeConverter implements Converter<Instant, LocalDateTime> {
    @Override
    public LocalDateTime convert(@NonNull Instant source) {
        return LocalDateTime.ofInstant(source, ZoneOffset.UTC);
    }
}
