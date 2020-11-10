package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.model.TaskComment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
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
    private final TaskCommentRepository taskCommentRepository;

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
}
