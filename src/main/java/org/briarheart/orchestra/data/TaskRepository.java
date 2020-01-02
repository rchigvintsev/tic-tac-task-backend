package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Task;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {
    @Query("SELECT * FROM task WHERE completed = :completed AND author = :author")
    Flux<Task> findByCompletedAndAuthor(Boolean completed, String author);

    @Query("SELECT * FROM task WHERE id= :id AND author = :author")
    Mono<Task> findByIdAndAuthor(Long id, String author);
}
