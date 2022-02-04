package org.briarheart.tictactask.task.tag;

import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface CustomizedTaskTagRelationRepository {
    Mono<TaskTagRelation> create(Long taskId, Long tagId);
}
