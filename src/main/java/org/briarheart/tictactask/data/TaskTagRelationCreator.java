package org.briarheart.tictactask.data;

import org.briarheart.tictactask.model.TaskTagRelation;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskTagRelationCreator {
    Mono<TaskTagRelation> create(Long taskId, Long tagId);
}
