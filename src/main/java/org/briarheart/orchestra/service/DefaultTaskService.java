package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Default implementation of {@link TaskService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultTaskService implements TaskService {
    private final TaskRepository taskRepository;

    @Override
    public Flux<Task> getUnprocessedTasks(String author) {
        return taskRepository.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(String author) {
        return taskRepository.findByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(LocalDateTime deadlineFrom, LocalDateTime deadlineTo, String author) {
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
        }
        if (deadlineFrom == null) {
            return taskRepository.findByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo, TaskStatus.PROCESSED,
                    author);
        }
        if (deadlineTo == null) {
            return taskRepository.findByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom, TaskStatus.PROCESSED,
                    author);
        }
        return taskRepository.findByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo, TaskStatus.PROCESSED,
                author);
    }

    @Override
    public Mono<Task> getTask(Long id, String author) throws EntityNotFoundException {
        return taskRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + id + " is not found")));
    }

    @Override
    public Mono<Task> createTask(Task task, String author) {
        Assert.notNull(task, "Task must not be null");
        Assert.hasText(author, "Task author must not be null or empty");
        return Mono.defer(() -> {
            Task newTask = task.copy();
            newTask.setAuthor(author);
            newTask.setStatus(TaskStatus.UNPROCESSED);
            return taskRepository.save(newTask);
        });
    }

    @Override
    public Mono<Task> updateTask(Task task, Long id, String author) throws EntityNotFoundException {
        Assert.notNull(task, "Task must not be null");
        return getTask(id, author).flatMap(t -> {
            task.setId(id);
            task.setAuthor(author);
            if (task.getStatus() == null) {
                task.setStatus(t.getStatus());
            }
            if (task.getStatus() == TaskStatus.UNPROCESSED && task.getDeadline() != null) {
                task.setStatus(TaskStatus.PROCESSED);
            }
            return taskRepository.save(task);
        });
    }

    @Override
    public Mono<Void> completeTask(Long id, String author) throws EntityNotFoundException {
        return getTask(id, author).flatMap(task -> {
            task.setStatus(TaskStatus.COMPLETED);
            return taskRepository.save(task);
        }).then();
    }
}
