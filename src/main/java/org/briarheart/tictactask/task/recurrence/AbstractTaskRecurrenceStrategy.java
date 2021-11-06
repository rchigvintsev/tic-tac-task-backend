package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public abstract class AbstractTaskRecurrenceStrategy implements TaskRecurrenceStrategy {
    private static final Logger log = LoggerFactory.getLogger(AbstractTaskRecurrenceStrategy.class);

    @Override
    public void reschedule(Task task) {
        Assert.notNull(task, "Task must not be null");
        doReschedule(task);
        log.debug("Task with id {} is rescheduled to {}", task.getId(), task.getDeadline());
    }

    protected abstract void doReschedule(Task task);
}
