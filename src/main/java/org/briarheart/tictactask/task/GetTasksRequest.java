package org.briarheart.tictactask.task;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class GetTasksRequest {
    private Set<TaskStatus> statuses;
    private boolean withoutDeadline;
    private LocalDate deadlineDateFrom;
    private LocalDate deadlineDateTo;
    private LocalDateTime deadlineDateTimeFrom;
    private LocalDateTime deadlineDateTimeTo;
    private LocalDateTime completedAtFrom;
    private LocalDateTime completedAtTo;
}
