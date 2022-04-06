package org.briarheart.tictactask.task.recurrence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Strategy interface to reschedule tasks.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DailyTaskRecurrenceStrategy.class, name = "daily"),
        @JsonSubTypes.Type(value = WeeklyTaskRecurrenceStrategy.class, name = "weekly"),
        @JsonSubTypes.Type(value = MonthlyTaskRecurrenceStrategy.class, name = "monthly"),
        @JsonSubTypes.Type(value = AnnuallyTaskRecurrenceStrategy.class, name = "annually")
})
public interface TaskRecurrenceStrategy {
}
