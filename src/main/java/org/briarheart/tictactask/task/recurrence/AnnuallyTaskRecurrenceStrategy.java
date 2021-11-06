package org.briarheart.tictactask.task.recurrence;

import org.briarheart.tictactask.task.Task;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneOffset;

public class AnnuallyTaskRecurrenceStrategy extends AbstractTaskRecurrenceStrategy {
    private Month month;
    private int dayOfMonth;

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    @Override
    protected void doReschedule(Task task) {
        LocalDateTime deadline = LocalDateTime.now(ZoneOffset.UTC)
                .plusYears(1)
                .withMonth(month.getValue())
                .withDayOfMonth(getDayOfMonthFromNextYear());
        task.setDeadline(deadline);
    }

    private int getDayOfMonthFromNextYear() {
        YearMonth nextYearMonth = YearMonth.now(ZoneOffset.UTC).plusYears(1).withMonth(month.getValue());
        return Math.min(dayOfMonth, nextYearMonth.lengthOfMonth());
    }
}
