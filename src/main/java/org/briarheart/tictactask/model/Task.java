package org.briarheart.tictactask.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
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
    private Long userId;
    private Long taskListId;
    @NotBlank
    @Size(max = 255)
    private String title;
    @Size(max = 10_000)
    private String description;
    private TaskStatus previousStatus;
    @Builder.Default
    private TaskStatus status = TaskStatus.UNPROCESSED;
    private LocalDateTime createdAt;
    @FutureOrPresent
    private LocalDateTime deadline;
    private boolean deadlineTimeExplicitlySet;

    /**
     * Creates copy of the given task.
     *
     * @param other task to be copied (must not be {@code null})
     */
    public Task(Task other) {
        Assert.notNull(other, "Task must not be null");
        this.id = other.id;
        this.userId = other.userId;
        this.taskListId = other.taskListId;
        this.title = other.title;
        this.description = other.description;
        this.previousStatus = other.previousStatus;
        this.status = other.status;
        this.createdAt = other.createdAt;
        this.deadline = other.deadline;
        this.deadlineTimeExplicitlySet = other.deadlineTimeExplicitlySet;
    }
}
