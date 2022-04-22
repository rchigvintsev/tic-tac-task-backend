package org.briarheart.tictactask.task.tag;

import io.jsonwebtoken.lang.Assert;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table("tag")
public class TaskTag {
    @Id
    private Long id;
    private Long userId;
    private String name;
    private Integer color;
    private LocalDateTime createdAt;

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
        this.createdAt = other.createdAt;
    }
}
