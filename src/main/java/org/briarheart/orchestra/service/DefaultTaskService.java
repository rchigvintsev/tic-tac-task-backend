package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

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
    public Mono<Long> getUnprocessedTaskCount(String author) {
        return taskRepository.countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Override
    public Flux<Task> getUnprocessedTasks(String author, Pageable pageable) {
        return taskRepository.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, Pageables.getOffset(pageable),
                Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getProcessedTaskCount(String author) {
        return taskRepository.countAllByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(String author, Pageable pageable) {
        return taskRepository.findByStatusAndAuthor(TaskStatus.PROCESSED, author, Pageables.getOffset(pageable),
                Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getProcessedTaskCount(LocalDate deadlineDateFrom, LocalDate deadlineDateTo, String author) {
        if (deadlineDateFrom == null && deadlineDateTo == null) {
            return taskRepository.countAllByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
        }
        if (deadlineDateFrom == null) {
            return taskRepository.countAllByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo,
                    TaskStatus.PROCESSED, author);
        }
        if (deadlineDateTo == null) {
            return taskRepository.countAllByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom,
                    TaskStatus.PROCESSED, author);
        }
        return taskRepository.countAllByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom, deadlineDateTo,
                TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(
            LocalDate deadlineDateFrom,
            LocalDate deadlineDateTo,
            String author,
            Pageable pageable
    ) {
        if (deadlineDateFrom == null && deadlineDateTo == null) {
            return taskRepository.findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                    Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineDateFrom == null) {
            return taskRepository.findByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo,
                    TaskStatus.PROCESSED, author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineDateTo == null) {
            return taskRepository.findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom,
                    TaskStatus.PROCESSED, author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        return taskRepository.findByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom, deadlineDateTo,
                TaskStatus.PROCESSED, author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
    }

    @Override
    public Mono<Long> getUncompletedTaskCount(String author) {
        return taskRepository.countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author);
    }

    @Override
    public Flux<Task> getUncompletedTasks(String author, Pageable pageable) {
        return taskRepository.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, Pageables.getOffset(pageable),
                Pageables.getLimit(pageable));
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
            if (newTask.getStatus() == null) {
                newTask.setStatus(TaskStatus.UNPROCESSED);
            }
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
            if (task.getStatus() == TaskStatus.UNPROCESSED && task.getDeadlineDate() != null) {
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
