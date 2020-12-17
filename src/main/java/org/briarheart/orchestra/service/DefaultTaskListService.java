package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskListRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.util.Pageables;
import org.springframework.data.domain.Pageable;
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
    private final TaskRepository taskRepository;

    @Override
    public Flux<TaskList> getUncompletedTaskLists(String author) {
        return taskListRepository.findByCompletedAndAuthor(false, author);
    }

    @Override
    public Mono<TaskList> getTaskList(Long id, String author) throws EntityNotFoundException {
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
    public Mono<TaskList> updateTaskList(TaskList taskList, Long id, String author) throws EntityNotFoundException {
        Assert.notNull(taskList, "Task list must not be null");
        return getTaskList(id, author).flatMap(savedTaskList -> {
            taskList.setId(savedTaskList.getId());
            taskList.setAuthor(savedTaskList.getAuthor());
            return taskListRepository.save(taskList);
        });
    }

    @Override
    public Mono<Void> deleteTaskList(Long id, String author) throws EntityNotFoundException {
        return getTaskList(id, author).flatMap(list -> taskListRepository.deleteByIdAndAuthor(id, author));
    }

    @Override
    public Flux<Task> getTasks(Long taskListId, String author, Pageable pageable) throws EntityNotFoundException {
        return getTaskList(taskListId, author).flatMapMany(taskList -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskRepository.findByTaskListIdAndAuthor(taskList.getId(), taskList.getAuthor(), offset, limit);
        });
    }
}
