package org.briarheart.tictactask.task.recurrence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TaskRecurrenceStrategyConverterTest {
    private TaskRecurrenceStrategyConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TaskRecurrenceStrategyConverter(new ObjectMapper());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenObjectMapperIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new TaskRecurrenceStrategyConverter(null));
        assertEquals("Object mapper must not be null", e.getMessage());
    }

    @Test
    void shouldSupportConvertingOfStringToTaskRecurrenceStrategy() {
        Set<GenericConverter.ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
        assertNotNull(convertibleTypes);
        boolean found = false;
        for (GenericConverter.ConvertiblePair convertiblePair : convertibleTypes) {
            if (convertiblePair.getSourceType() == String.class
                    && convertiblePair.getTargetType() == TaskRecurrenceStrategy.class) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Support of converting string to instance of " + TaskRecurrenceStrategy.class.getName()
                + " was expected");
    }

    @Test
    void shouldSupportConvertingOfTaskRecurrenceStrategyToString() {
        Set<GenericConverter.ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
        assertNotNull(convertibleTypes);
        boolean found = false;
        for (GenericConverter.ConvertiblePair convertiblePair : convertibleTypes) {
            if (convertiblePair.getSourceType() == TaskRecurrenceStrategy.class
                    && convertiblePair.getTargetType() == String.class) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Support of converting instance of " + TaskRecurrenceStrategy.class.getName()
                + " to string was expected");
    }

    @Test
    void shouldConvertTaskRecurrenceStrategyToJson() {
        TaskRecurrenceStrategy strategy = new DailyTaskRecurrenceStrategy();
        Object result = converter.convert(strategy, TypeDescriptor.valueOf(TaskRecurrenceStrategy.class),
                TypeDescriptor.valueOf(String.class));
        assertEquals("{\"type\":\"daily\"}", result);
    }

    @Test
    void shouldConvertJsonToTaskRecurrenceStrategy() {
        Object result = converter.convert("{\"type\":\"daily\"}", TypeDescriptor.valueOf(String.class),
                TypeDescriptor.valueOf(TaskRecurrenceStrategy.class));
        assertTrue(result instanceof DailyTaskRecurrenceStrategy);
    }

    @Test
    void shouldReturnNullOnConvertWhenSourceObjectIsNull() {
        Object result = converter.convert(null, TypeDescriptor.valueOf(String.class),
                TypeDescriptor.valueOf(TaskRecurrenceStrategy.class));
        assertNull(result);
    }
}