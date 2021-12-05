package org.briarheart.tictactask.task.comment;

import lombok.extern.slf4j.Slf4j;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Transactional
    @Override
    public Mono<TaskComment> updateComment(TaskComment comment) {
        Assert.notNull(comment, "Task comment must not be null");
        return taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Task comment with id " + comment.getId()
                        + " is not found")))
                .flatMap(existingComment -> {
                    comment.setTaskId(existingComment.getTaskId());
                    comment.setCreatedAt(existingComment.getCreatedAt());
                    comment.setUpdatedAt(getCurrentTime());
                    return taskCommentRepository.save(comment)
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
