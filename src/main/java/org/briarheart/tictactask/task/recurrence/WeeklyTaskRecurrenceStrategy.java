package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

public class WeeklyTaskRecurrenceStrategy extends AbstractTaskRecurrenceStrategy {
    private DayOfWeek dayOfWeek;

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    protected void doReschedule(Task task) {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC)
                .plusWeeks(1)
                .with(ChronoField.DAY_OF_WEEK, dayOfWeek.getValue());
        task.setDeadline(deadline);
    }
}
