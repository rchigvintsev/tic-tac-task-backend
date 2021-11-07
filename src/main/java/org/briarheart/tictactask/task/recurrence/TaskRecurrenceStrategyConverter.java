package org.briarheart.tictactask.task.recurrence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Assert;
import org.briarheart.tictactask.data.convert.converter.CustomConverter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Implementation of {@link CustomConverter} to convert instances of {@link TaskRecurrenceStrategy} to JSON and vice
 * versa.
 */
@Component
public class TaskRecurrenceStrategyConverter implements CustomConverter {
    private final ObjectMapper objectMapper;

    public TaskRecurrenceStrategyConverter(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "Object mapper must not be null");
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Set.of(
                new ConvertiblePair(TaskRecurrenceStrategy.class, String.class),
                new ConvertiblePair(String.class, TaskRecurrenceStrategy.class)
        );
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }

        return source instanceof String
                ? convertFromJson((String) source)
                : convertToJson((TaskRecurrenceStrategy) source);
    }

    private TaskRecurrenceStrategy convertFromJson(String json) {
        if (json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, TaskRecurrenceStrategy.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON string '" + json + "' to instance of "
                    + TaskRecurrenceStrategy.class.getName() + ": " + e.getMessage(), e);
        }
    }

    private String convertToJson(TaskRecurrenceStrategy recurrenceStrategy) {
        try {
            return objectMapper.writeValueAsString(recurrenceStrategy);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert instance of " + TaskRecurrenceStrategy.class.getName()
                    + " to JSON: " + e.getMessage(), e);
        }
    }
}
