package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;

/**
 * Strategy interface to reschedule tasks.
 */
public interface TaskRecurrenceStrategy {
    /**
     * Reschedules task by changing its deadline.
     *
     * @param task task to be rescheduled (must not be {@code null})
     */
    void reschedule(Task task);
}
