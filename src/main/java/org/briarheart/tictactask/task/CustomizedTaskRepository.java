package org.briarheart.tictactask.task;

import org.briarheart.tictactask.user.User;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomizedTaskRepository {
    Mono<Long> count(GetTasksRequest request, User user);

    Flux<Task> find(GetTasksRequest request, User user, Pageable pageable);
}
