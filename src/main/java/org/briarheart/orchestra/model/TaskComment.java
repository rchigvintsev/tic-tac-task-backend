package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;

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
     * Creates copy of this comment including all attributes except primary key.
     *
     * @return copy of this comment
     */
    public TaskComment copy() {
        TaskComment copy = new TaskComment();
        copy.setUserId(userId);
        copy.setTaskId(taskId);
        copy.setCommentText(commentText);
        copy.setCreatedAt(createdAt);
        copy.setUpdatedAt(updatedAt);
        return copy;
    }
}
