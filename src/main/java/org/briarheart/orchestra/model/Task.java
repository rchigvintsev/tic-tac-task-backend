package org.briarheart.orchestra.model;

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
    @Builder.Default
    private TaskStatus status = TaskStatus.UNPROCESSED;
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
        this.status = other.status;
        this.deadline = other.deadline;
        this.deadlineTimeExplicitlySet = other.deadlineTimeExplicitlySet;
    }

    /**
     * Creates copy of this task including all attributes except primary key.
     *
     * @return copy of this task
     */
    public Task copy() {
        Task copy = new Task();
        copy.setUserId(userId);
        copy.setTitle(title);
        copy.setDescription(description);
        copy.setStatus(status);
        copy.setDeadline(deadline);
        copy.setDeadlineTimeExplicitlySet(deadlineTimeExplicitlySet);
        copy.setTaskListId(taskListId);
        return copy;
    }
}
