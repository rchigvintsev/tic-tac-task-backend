package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskList;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskListRepository extends ReactiveCrudRepository<TaskList, Long> {
    @Query("SELECT * FROM task_list WHERE completed = :completed AND user_id = :userId")
    Flux<TaskList> findByCompletedAndUserId(boolean completed, Long userId);

    @Query("SELECT * FROM task_list WHERE id = :id AND user_id = :userId")
    Mono<TaskList> findByIdAndUserId(Long id, Long userId);
}
