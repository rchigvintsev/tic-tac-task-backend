package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskListRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.User;
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
    public Flux<TaskList> getUncompletedTaskLists(User user) {
        Assert.notNull(user, "User must not be null");
        return taskListRepository.findByCompletedAndUserId(false, user.getId());
    }

    @Override
    public Mono<TaskList> getTaskList(Long id, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return findTaskList(id, user.getId());
    }

    @Override
    public Mono<TaskList> createTaskList(TaskList taskList) {
        Assert.notNull(taskList, "Task list must not be null");
        return Mono.defer(() -> {
            TaskList newTaskList = new TaskList(taskList);
            newTaskList.setId(null);
            return taskListRepository.save(newTaskList);
        });
    }

    @Override
    public Mono<TaskList> updateTaskList(TaskList taskList) throws EntityNotFoundException {
        Assert.notNull(taskList, "Task list must not be null");
        return findTaskList(taskList.getId(), taskList.getUserId())
                .flatMap(existingTaskList -> taskListRepository.save(taskList));
    }

    @Override
    public Mono<Void> completeTaskList(Long id, User user) throws EntityNotFoundException {
        return getTaskList(id, user)
                .zipWhen(taskList -> {
                    Flux<Task> taskFlux = taskRepository.findByTaskListIdAndUserId(id, user.getId(), 0, null);
                    return taskFlux.flatMap(task -> {
                        task.setStatus(TaskStatus.COMPLETED);
                        return taskRepository.save(task);
                    }).then(Mono.just(true));
                })
                .flatMap(taskListAndFlag -> {
                    TaskList taskList = taskListAndFlag.getT1();
                    taskList.setCompleted(true);
                    return taskListRepository.save(taskList).then();
                });
    }

    @Override
    public Mono<Void> deleteTaskList(Long id, User user) throws EntityNotFoundException {
        return getTaskList(id, user)
                .zipWhen(taskList -> taskRepository.findByTaskListIdAndUserId(id, user.getId(), 0, null)
                        .flatMap(taskRepository::delete)
                        .then(Mono.just(true)))
                .flatMap(taskListAndFlag -> {
                    TaskList taskList = taskListAndFlag.getT1();
                    return taskListRepository.delete(taskList);
                });
    }

    @Override
    public Flux<Task> getTasks(Long taskListId, User user, Pageable pageable) throws EntityNotFoundException {
        return getTaskList(taskListId, user).flatMapMany(taskList -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), offset, limit);
        });
    }

    @Override
    public Mono<Void> addTask(Long taskListId, Long taskId, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return getTaskList(taskListId, user)
                .flatMap(taskList -> taskRepository.findByIdAndUserId(taskId, user.getId()))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + taskId + " is not found")))
                .flatMap(task -> {
                    task.setTaskListId(taskListId);
                    return taskRepository.save(task);
                })
                .then();
    }

    @Override
    public Mono<Void> removeTask(Long taskListId, Long taskId, User user) throws EntityNotFoundException {
        Assert.notNull(user, "User must not be null");
        return getTaskList(taskListId, user)
                .flatMap(taskList -> taskRepository.findByIdAndUserId(taskId, user.getId()))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + taskId + " is not found")))
                .flatMap(task -> {
                    task.setTaskListId(null);
                    return taskRepository.save(task);
                })
                .then();
    }

    private Mono<TaskList> findTaskList(Long taskListId, Long userId) {
        return taskListRepository.findByIdAndUserId(taskListId, userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task list with id " + taskListId
                        + " is not found")));
    }
}
