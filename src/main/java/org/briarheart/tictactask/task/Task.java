package org.briarheart.tictactask.task;

import lombok.*;
import org.briarheart.tictactask.task.recurrence.TaskRecurrenceStrategy;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Main entity representing task to be done.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Task {
    @Id
    private Long id;
    private Long parentId;
    private Long userId;
    private Long taskListId;
    private String title;
    private String description;
    private TaskStatus previousStatus;
    @Builder.Default
    private TaskStatus status = TaskStatus.UNPROCESSED;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDate deadlineDate;
    private LocalDateTime deadlineDateTime;
    private TaskRecurrenceStrategy recurrenceStrategy;

    /**
     * Creates copy of the given task.
     *
     * @param other task to be copied (must not be {@code null})
     */
    public Task(Task other) {
        Assert.notNull(other, "Task must not be null");
        this.id = other.id;
        this.parentId = other.parentId;
        this.userId = other.userId;
        this.taskListId = other.taskListId;
        this.title = other.title;
        this.description = other.description;
        this.previousStatus = other.previousStatus;
        this.status = other.status;
        this.createdAt = other.createdAt;
        this.completedAt = other.completedAt;
        this.deadlineDate = other.deadlineDate;
        this.deadlineDateTime = other.deadlineDateTime;
        this.recurrenceStrategy = other.recurrenceStrategy;
    }
}
