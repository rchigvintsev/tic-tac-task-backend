package org.briarheart.tictactask.task.list;

import lombok.*;
import org.springframework.data.annotation.Id;

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
