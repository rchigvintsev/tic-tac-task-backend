package org.briarheart.tictactask.task.tag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.briarheart.tictactask.task.Task;

import java.time.LocalDateTime;

/**
 * Many-to-many relation between {@link Task} and {@link TaskTag}.
 *
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTagRelation {
    private Long taskId;
    private Long tagId;
    private LocalDateTime createdAt;
}
