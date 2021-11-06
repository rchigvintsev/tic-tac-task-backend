package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Reschedules task to the next day.
 */
public class DailyTaskRecurrenceStrategy extends AbstractTaskRecurrenceStrategy {
    @Override
    protected void doReschedule(Task task) {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
        task.setDeadline(deadline);
    }
}
