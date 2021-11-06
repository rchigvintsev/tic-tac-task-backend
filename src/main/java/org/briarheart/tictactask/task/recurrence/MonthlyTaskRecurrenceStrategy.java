package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

public class MonthlyTaskRecurrenceStrategy extends AbstractTaskRecurrenceStrategy {
    private int dayOfMonth;

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    @Override
    protected void doReschedule(Task task) {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC).plusMonths(1).withDayOfMonth(getDayOfNextMonth());
        task.setDeadline(deadline);
    }

    private int getDayOfNextMonth() {
        YearMonth nextMonth = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        return Math.min(dayOfMonth, nextMonth.lengthOfMonth());
    }
}
