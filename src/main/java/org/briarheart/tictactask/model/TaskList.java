package org.briarheart.tictactask.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TaskList {
    @Id
    private Long id;
    private Long userId;
    @NotBlank
    @Size(max = 255)
    private String name;
    @Builder.Default
    private boolean completed = false;

    /**
     * Creates copy of the given task list.
     *
     * @param other task list to be copied (must not be {@code null})
     */
    public TaskList(TaskList other) {
        this.id = other.id;
        this.userId = other.userId;
        this.name = other.name;
        this.completed = other.completed;
    }
}
