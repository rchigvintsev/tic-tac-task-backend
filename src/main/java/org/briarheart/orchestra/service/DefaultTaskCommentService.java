package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.TaskComment;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Default implementation of {@link TaskCommentService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultTaskCommentService implements TaskCommentService {
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;

    @Override
    public Flux<TaskComment> getComments(Long taskId, String taskAuthor, Pageable pageable) {
        return taskRepository.findByIdAndAuthor(taskId, taskAuthor)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + taskId + " is not found")))
                .flatMapMany(task -> {
                    long offset = getOffset(pageable);
                    Integer limit = getLimit(pageable);
                    return taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(taskId, offset, limit);
                });
    }

    @Override
    public Mono<TaskComment> createComment(TaskComment comment, String commentAuthor, Long taskId) {
        Assert.notNull(comment, "Task comment must not be null");
        return taskRepository.findByIdAndAuthor(taskId, commentAuthor)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task with id " + taskId + " is not found")))
                .flatMap(task -> {
                    TaskComment newComment = comment.copy();
                    newComment.setTaskId(taskId);
                    newComment.setAuthor(commentAuthor);
                    newComment.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    return taskCommentRepository.save(newComment);
                });
    }

    @Override
    public Mono<TaskComment> updateComment(TaskComment comment, Long id, String author) {
        Assert.notNull(comment, "Task comment must not be null");
        return taskCommentRepository.findByIdAndAuthor(id, author)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task comment with id " + id + " is not found")))
                .flatMap(c -> {
                    comment.setId(c.getId());
                    comment.setTaskId(c.getTaskId());
                    comment.setCreatedAt(c.getCreatedAt());
                    comment.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    comment.setAuthor(author);
                    return taskCommentRepository.save(comment);
                });
    }

    @Override
    public Mono<Void> deleteComment(Long id, String author) {
        return taskCommentRepository.deleteByIdAndAuthor(id, author);
    }

    private long getOffset(Pageable pageable) {
        return pageable != null && pageable.isPaged() ? pageable.getOffset() : 0L;
    }

    private Integer getLimit(Pageable pageable) {
        return pageable != null && pageable.isPaged() ? pageable.getPageSize() : null;
    }
}
