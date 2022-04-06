package org.briarheart.tictactask.task.recurrence;

import lombok.Getter;
import lombok.Setter;

import java.time.Month;

@Getter
@Setter
public class AnnuallyTaskRecurrenceStrategy implements TaskRecurrenceStrategy {
    private Month month;
    private int dayOfMonth;
}
