package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyTaskRecurrenceStrategyTest {
    private MonthlyTaskRecurrenceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MonthlyTaskRecurrenceStrategy();
    }

    @Test
    void shouldRescheduleTaskToFirstDayOfMonth() {
        strategy.setDayOfMonth(1);

        Task task = new Task();
        task.setId(1L);

        strategy.reschedule(task);
        assertNotNull(task.getDeadlineDateTime());
        assertTrue(task.getDeadlineDateTime().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        assertEquals(1, task.getDeadlineDateTime().getDayOfMonth());
    }

    @Test
    void shouldRescheduleTaskToLastDayOfMonth() {
        strategy.setDayOfMonth(31);

        Task task = new Task();
        task.setId(1L);

        strategy.reschedule(task);
        assertNotNull(task.getDeadlineDateTime());
        assertTrue(task.getDeadlineDateTime().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        YearMonth nextMonth = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        assertEquals(nextMonth.lengthOfMonth(), task.getDeadlineDateTime().getDayOfMonth());
    }

    @Test
    void shouldThrowExceptionOnRescheduleWhenTaskIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> strategy.reschedule(null));
        assertEquals("Task must not be null", e.getMessage());
    }
}