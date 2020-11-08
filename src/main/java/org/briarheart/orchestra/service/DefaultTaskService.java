package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.*;
import org.briarheart.orchestra.model.*;
import org.briarheart.orchestra.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link TaskService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultTaskService implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskTagRelationRepository taskTagRelationRepository;
    private final TagRepository tagRepository;
    private final TaskCommentRepository taskCommentRepository;

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
    public Mono<Long> getProcessedTaskCount(LocalDateTime deadlineFrom, LocalDateTime deadlineTo, String author) {
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.countAllByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
        }
        if (deadlineFrom == null) {
            return taskRepository.countAllByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo, TaskStatus.PROCESSED,
                    author);
        }
        if (deadlineTo == null) {
            return taskRepository.countAllByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom,
                    TaskStatus.PROCESSED, author);
        }
        return taskRepository.countAllByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo,
                TaskStatus.PROCESSED, author);
    }

    @Override
    public Flux<Task> getProcessedTasks(
            LocalDateTime deadlineFrom,
            LocalDateTime deadlineTo,
            String author,
            Pageable pageable
    ) {
        if (deadlineFrom == null && deadlineTo == null) {
            return taskRepository.findByDeadlineIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                    Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineFrom == null) {
            return taskRepository.findByDeadlineLessThanEqualAndStatusAndAuthor(deadlineTo, TaskStatus.PROCESSED,
                    author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        if (deadlineTo == null) {
            return taskRepository.findByDeadlineGreaterThanEqualAndStatusAndAuthor(deadlineFrom, TaskStatus.PROCESSED,
                    author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
        }
        return taskRepository.findByDeadlineBetweenAndStatusAndAuthor(deadlineFrom, deadlineTo, TaskStatus.PROCESSED,
                author, Pageables.getOffset(pageable), Pageables.getLimit(pageable));
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
        return findTask(id, author);
    }

    @Override
    public Flux<Tag> getTags(Long taskId, String taskAuthor) throws EntityNotFoundException {
        return findTask(taskId, taskAuthor).flatMapMany(task -> {
            Mono<List<TaskTagRelation>> taskTagRelations = taskTagRelationRepository.findByTaskId(taskId).collectList();
            return taskTagRelations.flatMapMany(relationList -> {
                if (relationList.isEmpty()) {
                    return Flux.empty();
                }
                Set<Long> tagIds = relationList.stream()
                        .map(TaskTagRelation::getTagId)
                        .collect(Collectors.toSet());
                return tagRepository.findByIdIn(tagIds);
            });
        });
    }

    @Override
    public Mono<Void> assignTag(Long taskId, Long tagId, String author) throws EntityNotFoundException {
        return findTask(taskId, author)
                .flatMap(task -> findTag(tagId, author))
                .flatMap(tag -> taskTagRelationRepository.findByTaskIdAndTagId(taskId, tagId))
                .switchIfEmpty(Mono.defer(() -> taskTagRelationRepository.create(taskId, tagId)))
                .then();
    }

    @Override
    public Flux<TaskComment> getComments(Long taskId, String taskAuthor, Pageable pageable) {
        return findTask(taskId, taskAuthor).flatMapMany(task -> {
            long offset = Pageables.getOffset(pageable);
            Integer limit = Pageables.getLimit(pageable);
            return taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId, offset, limit);
        });
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
        return getTask(id, author).flatMap(savedTask -> {
            task.setId(savedTask.getId());
            task.setAuthor(savedTask.getAuthor());
            if (task.getStatus() == null) {
                task.setStatus(savedTask.getStatus());
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

    private Mono<Task> findTask(Long id, String author) {
        return taskRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + id + " is not found")));
    }

    private Mono<Tag> findTag(Long id, String author) {
        return tagRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Tag with id " + id + " is not found")));
    }
}
