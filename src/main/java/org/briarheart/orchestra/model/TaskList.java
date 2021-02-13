package org.briarheart.orchestra.model;

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
    private boolean completed;

    /**
     * Creates copy of this task list including all attributes except primary key.
     *
     * @return copy of this task list
     */
    public TaskList copy() {
        TaskList copy = new TaskList();
        copy.setUserId(userId);
        copy.setName(name);
        copy.setCompleted(completed);
        return copy;
    }
}
