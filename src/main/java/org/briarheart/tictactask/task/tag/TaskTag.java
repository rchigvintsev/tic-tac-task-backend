package org.briarheart.tictactask.task.tag;

import io.jsonwebtoken.lang.Assert;
import lombok.*;
import org.springframework.data.annotation.Id;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TaskTag {
    @Id
    private Long id;
    private Long userId;
    private String name;
    private Integer color;

    /**
     * Creates copy of the given tag.
     *
     * @param other tag to be copied (must not be {@code null})
     */
    public TaskTag(TaskTag other) {
        Assert.notNull(other, "Task tag must not be null");
        this.id = other.id;
        this.userId = other.userId;
        this.name = other.name;
        this.color = other.color;
    }
}
