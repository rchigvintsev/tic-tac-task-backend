package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskServiceTest {
    private TaskRepository taskRepositoryMock;
    private DefaultTaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepositoryMock = mock(TaskRepository.class);
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        taskService = new DefaultTaskService(taskRepositoryMock);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author)).thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.PROCESSED, author)).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateBetween() {
        LocalDate deadlineDateFrom = LocalDate.now();
        LocalDate deadlineDateTo = deadlineDateFrom.plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineDateBetweenAndStatusAndAuthor(
                deadlineDateFrom,
                deadlineDateTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineDateFrom, deadlineDateTo, author).blockFirst();
        verify(taskRepositoryMock, times(1))
                .findByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom, deadlineDateTo, TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDate deadlineDateTo = LocalDate.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineDateLessThanEqualAndStatusAndAuthor(
                deadlineDateTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, deadlineDateTo, author).blockFirst();
        verify(taskRepositoryMock, times(1))
                .findByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo, TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDate deadlineDateFrom = LocalDate.now();
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(
                deadlineDateFrom,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineDateFrom, null, author).blockFirst();
        verify(taskRepositoryMock, times(1))
                .findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom, TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepositoryMock.findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author))
                .thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, null, author).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author)).thenReturn(Flux.empty());

        taskService.getUncompletedTasks(author).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author);
    }

    @Test
    void shouldReturnTaskById() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task result = taskService.getTask(task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task, result);
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenTaskIsNotFound() {
        when(taskRepositoryMock.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTask(1L, "alice").block());
    }

    @Test
    void shouldCreateTask() {
        Task task = Task.builder().title("New task").build();
        Task result = taskService.createTask(task, "alice").block();
        assertNotNull(result);
        assertEquals(task.getTitle(), result.getTitle());
        verify(taskRepositoryMock, times(1)).save(any());
    }

    @Test
    void shouldSetAuthorFieldOnTaskCreate() {
        Task task = Task.builder().title("New task").build();
        String author = "alice";
        Task result = taskService.createTask(task, author).block();
        assertNotNull(result);
        assertEquals(author, result.getAuthor());
    }

    @Test
    void shouldSetTaskStatusToUnprocessedOnTaskCreate() {
        Task task = Task.builder().title("New task").status(null).build();
        Task result = taskService.createTask(task, "alice").block();
        assertNotNull(result);
        assertSame(TaskStatus.UNPROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskCreateWhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(null, null));
        assertEquals("Task must not be null", exception.getMessage());
    }

    @Test
    void shouldUpdateTask() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(updatedTask.getTitle(), result.getTitle());
    }

    @Test
    void shouldSetIdFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getId(), result.getId());
    }

    @Test
    void shouldSetAuthorFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldSetStatusFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").status(TaskStatus.UNPROCESSED).build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder().title("Updated test task").status(null).build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertSame(task.getStatus(), result.getStatus());
    }

    @Test
    void shouldMakeTaskProcessedOnTaskUpdateWhenDeadlineDateIsNotNull() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").status(TaskStatus.UNPROCESSED).build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Task updatedTask = Task.builder()
                .title("Updated test task")
                .deadlineDate(LocalDate.now().plus(3, ChronoUnit.DAYS))
                .build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertSame(TaskStatus.PROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskUpdateWhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.updateTask(null, null, null));
        assertEquals("Task must not be null", exception.getMessage());
    }

    @Test
    void shouldCompleteTask() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        taskService.completeTask(task.getId(), task.getAuthor()).block();
        assertSame(TaskStatus.COMPLETED, task.getStatus());
    }
}