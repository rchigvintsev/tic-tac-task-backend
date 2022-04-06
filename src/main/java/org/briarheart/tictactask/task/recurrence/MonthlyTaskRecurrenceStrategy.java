package org.briarheart.tictactask.task.recurrence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyTaskRecurrenceStrategy implements TaskRecurrenceStrategy {
    private int dayOfMonth;
}
