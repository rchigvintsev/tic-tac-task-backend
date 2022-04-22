package org.briarheart.tictactask.task.comment;

import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskCommentServiceTest {
    private TaskCommentRepository taskCommentRepository;
    private DefaultTaskCommentService taskCommentService;
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        taskCommentRepository = mock(TaskCommentRepository.class);

        currentTime = DateTimeUtils.currentDateTimeUtc();
        taskCommentService = new DefaultTaskCommentService(taskCommentRepository) {
            @Override
            protected LocalDateTime getCurrentTime() {
                return currentTime;
            }
        };
    }

    @Test
    void shouldUpdateComment() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .userId(2L)
                .taskId(3L)
                .commentText("Test comment")
                .createdAt(currentTime)
                .build();

        when(taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any())).thenAnswer(args -> Mono.just(new TaskComment(args.getArgument(0))));

        TaskComment updatedComment = new TaskComment(comment);
        updatedComment.setCommentText("Updated test comment");
        updatedComment.setCreatedAt(null);
        updatedComment.setTaskId(null);

        TaskComment expectedResult = new TaskComment(comment);
        expectedResult.setCommentText(updatedComment.getCommentText());
        expectedResult.setUpdatedAt(currentTime);

        TaskComment result = taskCommentService.updateComment(updatedComment).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeCreatedAtFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .userId(2L)
                .taskId(3L)
                .commentText("Test comment")
                .createdAt(currentTime)
                .build();

        when(taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any())).thenAnswer(args -> Mono.just(new TaskComment(args.getArgument(0))));

        TaskComment updatedComment = new TaskComment(comment);
        updatedComment.setCreatedAt(currentTime.minus(1, ChronoUnit.HOURS));

        TaskComment expectedResult = new TaskComment(comment);
        expectedResult.setUpdatedAt(currentTime);

        TaskComment result = taskCommentService.updateComment(updatedComment).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeTaskIdFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .userId(2L)
                .taskId(3L)
                .commentText("Test comment")
                .createdAt(currentTime)
                .build();

        when(taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any())).thenAnswer(args -> Mono.just(new TaskComment(args.getArgument(0))));

        TaskComment updatedComment = new TaskComment(comment);
        updatedComment.setTaskId(4L);

        TaskComment expectedResult = new TaskComment(comment);
        expectedResult.setUpdatedAt(currentTime);

        TaskComment result = taskCommentService.updateComment(updatedComment).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnCommentUpdateWhenCommentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskCommentService.updateComment(null));
        assertEquals("Task comment must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCommentUpdateWhenCommentIsNotFound() {
        when(taskCommentRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        TaskComment comment = TaskComment.builder().id(1L).userId(2L).commentText("Updated test comment").build();
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskCommentService.updateComment(comment).block());
        assertEquals("Task comment with id " + comment.getId() + " is not found", exception.getMessage());
    }

    @Test
    void shouldDeleteComment() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        long commentId = 2L;
        when(taskCommentRepository.deleteByIdAndUserId(commentId, user.getId())).thenReturn(Mono.just(true).then());
        taskCommentService.deleteComment(commentId, user).block();
        verify(taskCommentRepository, times(1)).deleteByIdAndUserId(commentId, user.getId());
    }

    @Test
    void shouldThrowExceptionOnCommentDeleteWhenUserIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskCommentService.deleteComment(1L, null).block());
        assertEquals("User must not be null", exception.getMessage());
    }
}
