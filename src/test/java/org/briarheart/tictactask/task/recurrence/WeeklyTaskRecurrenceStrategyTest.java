package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyTaskRecurrenceStrategyTest {
    private WeeklyTaskRecurrenceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new WeeklyTaskRecurrenceStrategy();
    }

    @Test
    void shouldRescheduleTaskToNextMonday() {
        strategy.setDayOfWeek(DayOfWeek.MONDAY);

        Task task = new Task();
        task.setId(1L);

        strategy.reschedule(task);
        assertNotNull(task.getDeadlineDateTime());
        assertTrue(task.getDeadlineDateTime().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        assertSame(DayOfWeek.MONDAY, task.getDeadlineDateTime().getDayOfWeek());
    }

    @Test
    void shouldThrowExceptionOnRescheduleWhenTaskIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> strategy.reschedule(null));
        assertEquals("Task must not be null", e.getMessage());
    }
}