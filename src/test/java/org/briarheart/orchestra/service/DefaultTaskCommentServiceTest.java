package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.model.TaskComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
                .author("alice")
                .commentText("Test comment")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        when(taskCommentRepository.findByIdAndAuthor(comment.getId(), comment.getAuthor()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment updatedComment = TaskComment.builder().commentText("Updated test comment").build();
        TaskComment result = taskCommentService.updateComment(updatedComment, comment.getId(), comment.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(updatedComment.getCommentText(), result.getCommentText());
    }

    @Test
    void shouldSetIdFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder().id(1L).author("alice").commentText("Test comment").build();
        when(taskCommentRepository.findByIdAndAuthor(comment.getId(), comment.getAuthor()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment updatedComment = TaskComment.builder().commentText("Updated test comment").build();
        TaskComment result = taskCommentService.updateComment(updatedComment, comment.getId(), comment.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
    }

    @Test
    void shouldSetTaskIdFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .taskId(2L)
                .author("alice")
                .commentText("Test comment")
                .build();
        when(taskCommentRepository.findByIdAndAuthor(comment.getId(), comment.getAuthor()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment updatedComment = TaskComment.builder().commentText("Updated test comment").build();
        TaskComment result = taskCommentService.updateComment(updatedComment, comment.getId(), comment.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(comment.getTaskId(), result.getTaskId());
    }

    @Test
    void shouldSetCreatedAtFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .author("alice")
                .commentText("Test comment")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        when(taskCommentRepository.findByIdAndAuthor(comment.getId(), comment.getAuthor()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment updatedComment = TaskComment.builder().commentText("Updated test comment").build();
        TaskComment result = taskCommentService.updateComment(updatedComment, comment.getId(), comment.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(comment.getCreatedAt(), result.getCreatedAt());
    }

    @Test
    void shouldSetUpdatedAtFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder().id(1L).author("alice").commentText("Test comment").build();
        when(taskCommentRepository.findByIdAndAuthor(comment.getId(), comment.getAuthor()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment updatedComment = TaskComment.builder().commentText("Updated test comment").build();
        TaskComment result = taskCommentService.updateComment(updatedComment, comment.getId(), comment.getAuthor())
                .block();
        assertNotNull(result);
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void shouldSetAuthorFieldOnCommentUpdate() {
        TaskComment comment = TaskComment.builder().id(1L).author("alice").commentText("Test comment").build();
        when(taskCommentRepository.findByIdAndAuthor(comment.getId(), comment.getAuthor()))
                .thenReturn(Mono.just(comment));
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment updatedComment = TaskComment.builder().commentText("Updated test comment").build();
        TaskComment result = taskCommentService.updateComment(updatedComment, comment.getId(), comment.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(comment.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnCommentUpdateWhenCommentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskCommentService.updateComment(null, null, null));
        assertEquals("Task comment must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCommentUpdateWhenCommentIsNotFound() {
        when(taskCommentRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long taskCommentId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskCommentService.updateComment(new TaskComment(), taskCommentId, "alice").block());
        assertEquals("Task comment with id " + taskCommentId + " is not found", exception.getMessage());
    }

    @Test
    void shouldDeleteComment() {
        Long commentId = 1L;
        String commentAuthor = "alice";
        when(taskCommentRepository.deleteByIdAndAuthor(commentId, commentAuthor)).thenReturn(Mono.empty());
        taskCommentService.deleteComment(commentId, commentAuthor).block();
        verify(taskCommentRepository, times(1)).deleteByIdAndAuthor(commentId, commentAuthor);
    }
}
