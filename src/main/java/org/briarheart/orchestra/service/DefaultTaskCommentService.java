package org.briarheart.orchestra.service;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.model.User;
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
    public Mono<TaskComment> updateComment(TaskComment comment) {
        Assert.notNull(comment, "Task comment must not be null");
        return taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task comment with id " + comment.getId()
                        + " is not found")))
                .flatMap(c -> {
                    comment.setTaskId(c.getTaskId());
                    comment.setCreatedAt(c.getCreatedAt());
                    comment.setUpdatedAt(getCurrentTime());
                    return taskCommentRepository.save(comment);
                });
    }

    @Override
    public Mono<Void> deleteComment(Long id, User user) {
        Assert.notNull(user, "User must not be null");
        return taskCommentRepository.deleteByIdAndUserId(id, user.getId());
    }

    protected LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
