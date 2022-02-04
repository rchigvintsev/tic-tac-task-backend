package org.briarheart.tictactask.task;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@EqualsAndHashCode
public class GetTasksRequest {
    @Getter
    @Setter
    private Set<TaskStatus> statuses;

    @Getter
    private LocalDateTime deadlineFrom;

    @Getter(AccessLevel.PACKAGE)
    private boolean deadlineFromDirty;

    @Getter
    private LocalDateTime deadlineTo;

    @Getter(AccessLevel.PACKAGE)
    private boolean deadlineToDirty;

    @Getter
    @Setter
    private LocalDateTime completedAtFrom;

    @Getter
    @Setter
    private LocalDateTime completedAtTo;

    public void setDeadlineFrom(LocalDateTime deadlineFrom) {
        this.deadlineFrom = deadlineFrom;
        this.deadlineFromDirty = true;
    }

    public void setDeadlineTo(LocalDateTime deadlineTo) {
        this.deadlineTo = deadlineTo;
        this.deadlineToDirty = true;
    }
}
