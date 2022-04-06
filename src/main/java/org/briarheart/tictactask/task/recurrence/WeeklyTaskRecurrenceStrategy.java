package org.briarheart.tictactask.task.recurrence;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;

@Getter
@Setter
public class WeeklyTaskRecurrenceStrategy implements TaskRecurrenceStrategy {
    private DayOfWeek dayOfWeek;
}
