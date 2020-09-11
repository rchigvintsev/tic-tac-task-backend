package org.briarheart.orchestra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Many-to-many relation between {@link Task} and {@link Tag}.
 *
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTagRelation {
    private Long taskId;
    private Long tagId;
}
