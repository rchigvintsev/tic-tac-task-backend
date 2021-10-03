package org.briarheart.tictactask.data;

import org.briarheart.tictactask.model.TaskTagRelation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskTagRelationRepository
        extends ReactiveCrudRepository<TaskTagRelation, Void>, TaskTagRelationCreator {
    @Query("SELECT * FROM tasks_tags WHERE task_id = :taskId AND tag_id = :tagId")
    Mono<TaskTagRelation> findByTaskIdAndTagId(Long taskId, Long tagId);

    @Query("SELECT * FROM tasks_tags WHERE task_id = :taskId")
    Flux<TaskTagRelation> findByTaskId(Long taskId);

    @Query("DELETE FROM tasks_tags WHERE task_id = :taskId AND tag_id = :tagId")
    Mono<Void> deleteByTaskIdAndTagId(Long taskId, Long tagId);

    @Query("DELETE FROM tasks_tags WHERE tag_id = :tagId")
    Mono<Void> deleteByTagId(Long tagId);
}
