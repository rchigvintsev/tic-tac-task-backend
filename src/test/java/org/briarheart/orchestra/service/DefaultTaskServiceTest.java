package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        taskService = new DefaultTaskService(taskRepositoryMock);
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByCompletedAndAuthor(true, author)).thenReturn(Flux.empty());

        taskService.getCompletedTasks(author).blockFirst();
        verify(taskRepositoryMock, times(1)).findByCompletedAndAuthor(true, author);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByCompletedAndAuthor(false, author)).thenReturn(Flux.empty());

        taskService.getUncompletedTasks(author).blockFirst();
        verify(taskRepositoryMock, times(1)).findByCompletedAndAuthor(false, author);
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
        long expectedTaskId = 2L;
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0, Task.class);
            savedTask.setId(expectedTaskId);
            return Mono.just(savedTask);
        });

        String author = "alice";
        Task result = taskService.createTask(task, author).block();
        assertNotNull(result);
        assertEquals(expectedTaskId, result.getId());
        assertEquals(task.getTitle(), result.getTitle());
    }

    @Test
    void shouldSetAuthorFieldOnTaskCreate() {
        Task task = Task.builder().title("New task").build();
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        String author = "alice";
        Task result = taskService.createTask(task, author).block();
        assertNotNull(result);
        assertEquals(author, result.getAuthor());
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
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(updatedTask.getTitle(), result.getTitle());
    }

    @Test
    void shouldSetIdFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getId(), result.getId());
    }

    @Test
    void shouldSetAuthorFieldOnTaskUpdate() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        Task updatedTask = Task.builder().title("Updated test task").build();
        Task result = taskService.updateTask(updatedTask, task.getId(), task.getAuthor()).block();
        assertNotNull(result);
        assertEquals(task.getAuthor(), result.getAuthor());
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
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));

        taskService.completeTask(task.getId(), task.getAuthor()).block();
        assertEquals(true, task.getCompleted());
    }
}
