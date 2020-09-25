package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
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
    private TaskRepository taskRepositoryMock;
    private TaskCommentRepository taskCommentRepository;
    private DefaultTaskCommentService taskCommentService;

    @BeforeEach
    void setUp() {
        taskRepositoryMock = mock(TaskRepository.class);
        taskCommentRepository = mock(TaskCommentRepository.class);
        taskCommentService = new DefaultTaskCommentService(taskRepositoryMock, taskCommentRepository);
    }

    @Test
    void shouldReturnAllCommentsForTask() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskCommentService.getComments(task.getId(), task.getAuthor(), Pageable.unpaged()).blockFirst();
        verify(taskCommentRepository, times(1)).findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null);
    }

    @Test
    void shouldReturnAllCommentsForTaskWithPagingRestriction() {
        PageRequest pageRequest = PageRequest.of(3, 50);

        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskCommentService.getComments(task.getId(), task.getAuthor(), pageRequest).blockFirst();
        verify(taskCommentRepository, times(1)).findByTaskIdOrderByCreatedAtDesc(task.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldThrowExceptionOnCommentsGetWhenTaskIsNotFound() {
        when(taskRepositoryMock.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class,
                () -> taskCommentService.getComments(1L, "alice", Pageable.unpaged()).blockFirst());
    }

    @Test
    void shouldCreateComment() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        long expectedTaskCommentId = 2L;
        when(taskCommentRepository.save(any())).thenAnswer(invocation -> {
            TaskComment savedComment = invocation.getArgument(0, TaskComment.class);
            savedComment.setId(expectedTaskCommentId);
            return Mono.just(savedComment);
        });
        TaskComment newComment = TaskComment.builder().commentText("New test comment").build();

        TaskComment result = taskCommentService.createComment(newComment, task.getAuthor(), task.getId()).block();
        assertNotNull(result);
        assertEquals(expectedTaskCommentId, result.getId());
        assertEquals(newComment.getCommentText(), result.getCommentText());
    }

    @Test
    void shouldSetTaskIdFieldOnTaskCommentCreate() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        TaskComment newComment = TaskComment.builder().commentText("New test comment").build();
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment result = taskCommentService.createComment(newComment, task.getAuthor(), task.getId()).block();
        assertNotNull(result);
        assertEquals(task.getId(), result.getTaskId());
    }

    @Test
    void shouldSetAuthorFieldOnTaskCommentCreate() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        TaskComment newComment = TaskComment.builder().commentText("New test comment").build();
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment result = taskCommentService.createComment(newComment, task.getAuthor(), task.getId()).block();
        assertNotNull(result);
        assertEquals(task.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldSetCreatedAtFieldOnTaskCommentCreate() {
        Task task = Task.builder().id(1L).author("alice").title("Test task").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        TaskComment newComment = TaskComment.builder().commentText("New test comment").build();
        when(taskCommentRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskComment.class)));

        TaskComment result = taskCommentService.createComment(newComment, task.getAuthor(), task.getId()).block();
        assertNotNull(result);
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionOnCommentCreateWhenCommentIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskCommentService.createComment(null, null, null));
        assertEquals("Task comment must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCommentCreateWhenTaskIsNotFound() {
        when(taskRepositoryMock.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long taskId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskCommentService.createComment(new TaskComment(), "alice", taskId).block());
        assertEquals("Task with id " + taskId + " is not found", exception.getMessage());
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
