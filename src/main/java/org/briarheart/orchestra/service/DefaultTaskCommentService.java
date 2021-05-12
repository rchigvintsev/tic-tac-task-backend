package org.briarheart.orchestra.service;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DefaultTaskCommentService implements TaskCommentService {
    private final TaskCommentRepository taskCommentRepository;

    public DefaultTaskCommentService(TaskCommentRepository taskCommentRepository) {
        Assert.notNull(taskCommentRepository, "Task comment repository must not be null");
        this.taskCommentRepository = taskCommentRepository;
    }

    @Override
    public Mono<TaskComment> updateComment(TaskComment comment) {
        Assert.notNull(comment, "Task comment must not be null");
        return taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task comment with id " + comment.getId()
                        + " is not found")))
                .flatMap(c -> {
                    TaskComment newComment = new TaskComment(comment);
                    newComment.setTaskId(c.getTaskId());
                    newComment.setCreatedAt(c.getCreatedAt());
                    newComment.setUpdatedAt(getCurrentTime());
                    return taskCommentRepository.save(newComment)
                            .doOnSuccess(result -> log.debug("Task comment with id {} is updated", result.getId()));
                });
    }

    @Override
    public Mono<Void> deleteComment(Long id, User user) {
        Assert.notNull(user, "User must not be null");
        return taskCommentRepository.deleteByIdAndUserId(id, user.getId())
                .doOnSuccess(v -> log.debug("Task comment with id {} is deleted", id));
    }

    protected LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
