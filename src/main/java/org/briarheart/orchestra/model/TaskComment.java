package org.briarheart.orchestra.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Task comment.
 *
 * @author Roman Chigvintsev
 */
@Entity
@Data
@NoArgsConstructor
public class TaskComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @NotBlank(message = "{javax.validation.constraints.NotBlank.commentText.message}")
    @Size(max = 10_000, message = "{javax.validation.constraints.Size.commentText.message}")
    private String commentText;

    @NotNull(message = "{javax.validation.constraints.NotNull.createdAt.message}")
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
