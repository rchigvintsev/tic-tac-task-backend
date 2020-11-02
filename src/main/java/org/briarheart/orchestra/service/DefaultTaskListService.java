package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskListRepository;
import org.briarheart.orchestra.model.TaskList;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link TaskListService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultTaskListService implements TaskListService {
    private final TaskListRepository taskListRepository;

    @Override
    public Flux<TaskList> getUncompletedTaskLists(String author) {
        return taskListRepository.findByCompletedAndAuthor(false, author);
    }

    public Mono<TaskList> getTaskList(Long id, String author) {
        return taskListRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task list with id " + id + " is not found")));
    }

    @Override
    public Mono<TaskList> createTaskList(TaskList taskList, String author) {
        Assert.notNull(taskList, "Task list must not be null");
        Assert.hasText(author, "Task list author must not be null or empty");
        return Mono.defer(() -> {
            TaskList newTaskList = taskList.copy();
            newTaskList.setAuthor(author);
            return taskListRepository.save(newTaskList);
        });
    }

    @Override
    public Mono<Void> deleteTaskList(Long id, String author) {
        return getTaskList(id, author).flatMap(list -> taskListRepository.deleteByIdAndAuthor(id, author));
    }
}
