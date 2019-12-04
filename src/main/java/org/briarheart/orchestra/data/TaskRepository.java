package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Task;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * @author Roman Chigvintsev
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {
    @Query("SELECT * FROM Task WHERE completed = :completed")
    Flux<Task> findByCompleted(Boolean completed);
}
