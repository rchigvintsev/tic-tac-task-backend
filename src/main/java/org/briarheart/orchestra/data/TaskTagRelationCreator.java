package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskTagRelation;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskTagRelationCreator {
    Mono<TaskTagRelation> create(Long taskId, Long tagId);
}
