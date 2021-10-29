package org.briarheart.tictactask.task.comment;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Task comment.
 *
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TaskComment {
    @Id
    private Long id;
    private Long userId;
    private Long taskId;
    @NotBlank
    @Size(max = 10_000)
    private String commentText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Creates copy of the given task comment.
     *
     * @param other task comment to be copied (must not be {@code null})
     */
    public TaskComment(TaskComment other) {
        Assert.notNull(other, "Task comment must not be null");
        this.id = other.id;
        this.userId = other.userId;
        this.taskId = other.taskId;
        this.commentText = other.commentText;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
    }
}
