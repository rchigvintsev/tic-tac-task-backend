package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AnnuallyTaskRecurrenceStrategyTest {
    private AnnuallyTaskRecurrenceStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AnnuallyTaskRecurrenceStrategy();
    }

    @Test
    void shouldRescheduleTaskToFirstDayOfJanuary() {
        strategy.setMonth(Month.JANUARY);
        strategy.setDayOfMonth(1);

        Task task = new Task();
        task.setId(1L);

        strategy.reschedule(task);
        assertNotNull(task.getDeadline());
        assertTrue(task.getDeadline().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        assertSame(Month.JANUARY, task.getDeadline().getMonth());
        assertEquals(1, task.getDeadline().getDayOfMonth());
    }

    @Test
    void shouldRescheduleTaskToLastDayOfJanuary() {
        strategy.setMonth(Month.JANUARY);
        strategy.setDayOfMonth(31);

        Task task = new Task();
        task.setId(1L);

        strategy.reschedule(task);
        assertNotNull(task.getDeadline());
        assertTrue(task.getDeadline().isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        assertSame(Month.JANUARY, task.getDeadline().getMonth());
        assertEquals(31, task.getDeadline().getDayOfMonth());
    }

    @Test
    void shouldThrowExceptionOnRescheduleWhenTaskIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> strategy.reschedule(null));
        assertEquals("Task must not be null", e.getMessage());
    }
}