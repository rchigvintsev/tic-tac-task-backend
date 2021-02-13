package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskCommentServiceTest {
    private TaskCommentRepository taskCommentRepository;
    private DefaultTaskCommentService taskCommentService;

    @BeforeEach
    void setUp() {
        taskCommentRepository = mock(TaskCommentRepository.class);
        taskCommentService = new DefaultTaskCommentService(taskCommentRepository);
    }

    @Test
    void shouldUpdateComment() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .taskId(2L)
                .userId(2L)
                .commentText("Test comment")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        when(taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        TaskComment updatedComment = comment.copy();
        updatedComment.setCommentText("Updated test comment");
        TaskComment result = taskCommentService.updateComment(updatedComment).block();
        assertNotNull(result);
        assertEquals(updatedComment.getCommentText(), result.getCommentText());
    }

    @Test
    void shouldSetCreatedAtFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .userId(2L)
                .userId(3L)
                .commentText("Test comment")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        when(taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        TaskComment updatedComment = comment.copy();
        updatedComment.setCommentText("Updated test comment");
        updatedComment.setCreatedAt(null);

        TaskComment result = taskCommentService.updateComment(updatedComment).block();
        assertNotNull(result);
        assertEquals(comment.getCreatedAt(), result.getCreatedAt());
    }

    @Test
    void shouldSetUpdatedAtFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .userId(2L)
                .userId(3L)
                .commentText("Test comment")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        when(taskCommentRepository.findByIdAndUserId(comment.getId(), comment.getUserId()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        TaskComment updatedComment = comment.copy();
        updatedComment.setCommentText("Updated test comment");

        TaskComment result = taskCommentService.updateComment(updatedComment).block();
        assertNotNull(result);
        assertNotNull(result.getUpdatedAt());
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
        TaskComment comment = TaskComment.builder().id(1L).userId(2L).build();
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskCommentService.updateComment(comment).block());
        assertEquals("Task comment with id " + comment.getId() + " is not found", exception.getMessage());
    }

    @Test
    void shouldDeleteComment() {
        User user = User.builder().id(1L).build();
        Long commentId = 1L;
        when(taskCommentRepository.deleteByIdAndUserId(commentId, user.getId())).thenReturn(Mono.empty());
        taskCommentService.deleteComment(commentId, user).block();
        verify(taskCommentRepository, times(1)).deleteByIdAndUserId(commentId, user.getId());
    }
}
