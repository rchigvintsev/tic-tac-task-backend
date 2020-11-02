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

    @NotBlank
    @Size(max = 255)
    private String name;

    private String author;
    private boolean completed;

    /**
     * Creates copy of this task list including all attributes except primary key.
     *
     * @return copy of this task list
     */
    public TaskList copy() {
        TaskList copy = new TaskList();
        copy.setName(name);
        copy.setAuthor(author);
        return copy;
    }
}
