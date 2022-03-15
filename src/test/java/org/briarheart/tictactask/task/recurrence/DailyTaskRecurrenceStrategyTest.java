package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class DailyTaskRecurrenceStrategyTest {
    private DailyTaskRecurrenceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DailyTaskRecurrenceStrategy();
    }

    @Test
    void shouldRescheduleTaskToNextDay() {
        Task task = new Task();
        task.setId(1L);
        strategy.reschedule(task);

        LocalDateTime expectedDeadline = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        assertNotNull(task.getDeadlineDateTime());
        assertEquals(expectedDeadline.getDayOfYear(), task.getDeadlineDateTime().getDayOfYear());
    }

    @Test
    void shouldThrowExceptionOnRescheduleWhenTaskIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> strategy.reschedule(null));
        assertEquals("Task must not be null", e.getMessage());
    }
}