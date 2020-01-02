package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

    private Long taskId;

    @NotBlank(message = "{javax.validation.constraints.NotBlank.commentText.message}")
    @Size(max = 10_000, message = "{javax.validation.constraints.Size.commentText.message}")
    private String commentText;

    @NotNull(message = "{javax.validation.constraints.NotNull.createdAt.message}")
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @NotNull(message = "{javax.validation.constraints.NotNull.author.message}")
    @Size(max = 254, message = "{javax.validation.constraints.Size.author.message}")
    private String author;

    /**
     * Creates copy of this comment including all attributes except primary key.
     *
     * @return copy of this comment
     */
    public TaskComment copy() {
        TaskComment copy = new TaskComment();
        copy.setTaskId(taskId);
        copy.setCommentText(commentText);
        copy.setCreatedAt(createdAt);
        copy.setUpdatedAt(updatedAt);
        copy.setAuthor(author);
        return copy;
    }
}
