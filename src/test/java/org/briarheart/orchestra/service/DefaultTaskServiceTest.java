package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TagRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private TagRepository tagRepositoryMock;
    private DefaultTaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepositoryMock = mock(TaskRepository.class);
        when(taskRepositoryMock.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));
        tagRepositoryMock = mock(TagRepository.class);
        taskService = new DefaultTaskService(taskRepositoryMock, tagRepositoryMock);
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author)).thenReturn(Mono.empty());
        taskService.getUnprocessedTaskCount(author).block();
        verify(taskRepositoryMock, times(1)).countAllByStatusAndAuthor(TaskStatus.UNPROCESSED, author);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnAllUnprocessedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.UNPROCESSED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUnprocessedTasks(author, pageRequest).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.UNPROCESSED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.countAllByStatusAndAuthor(TaskStatus.PROCESSED, author)).thenReturn(Mono.empty());
        taskService.getProcessedTaskCount(author).block();
        verify(taskRepositoryMock, times(1)).countAllByStatusAndAuthor(TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null)).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnAllProcessedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepositoryMock.findByStatusAndAuthor(TaskStatus.PROCESSED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getProcessedTasks(author, pageRequest).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusAndAuthor(TaskStatus.PROCESSED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateBetween() {
        LocalDate deadlineDateFrom = LocalDate.now();
        LocalDate deadlineDateTo = deadlineDateFrom.plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.countAllByDeadlineDateBetweenAndStatusAndAuthor(
                deadlineDateFrom,
                deadlineDateTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineDateFrom, deadlineDateTo, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom,
                deadlineDateTo, TaskStatus.PROCESSED, author);
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
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineDateFrom, deadlineDateTo, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineDateBetweenAndStatusAndAuthor(deadlineDateFrom,
                deadlineDateTo, TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDate deadlineDateTo = LocalDate.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.countAllByDeadlineDateLessThanEqualAndStatusAndAuthor(
                deadlineDateTo,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, deadlineDateTo, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo,
                TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateLessThanOrEqual() {
        LocalDate deadlineDateTo = LocalDate.now().plus(1, ChronoUnit.DAYS);
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineDateLessThanEqualAndStatusAndAuthor(
                deadlineDateTo,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, deadlineDateTo, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineDateLessThanEqualAndStatusAndAuthor(deadlineDateTo,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDate deadlineDateFrom = LocalDate.now();
        String author = "alice";

        when(taskRepositoryMock.countAllByDeadlineDateGreaterThanEqualAndStatusAndAuthor(
                deadlineDateFrom,
                TaskStatus.PROCESSED,
                author
        )).thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(deadlineDateFrom, null, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom,
                TaskStatus.PROCESSED, author);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadlineDateGreaterThanOrEqual() {
        LocalDate deadlineDateFrom = LocalDate.now();
        String author = "alice";

        when(taskRepositoryMock.findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(
                deadlineDateFrom,
                TaskStatus.PROCESSED,
                author,
                0,
                null
        )).thenReturn(Flux.empty());

        taskService.getProcessedTasks(deadlineDateFrom, null, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineDateGreaterThanEqualAndStatusAndAuthor(deadlineDateFrom,
                TaskStatus.PROCESSED, author, 0, null);
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepositoryMock.countAllByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author))
                .thenReturn(Mono.empty());

        taskService.getProcessedTaskCount(null, null, author).block();
        verify(taskRepositoryMock, times(1)).countAllByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED,
                author);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadlineDate() {
        String author = "alice";
        when(taskRepositoryMock.findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getProcessedTasks(null, null, author, null).blockFirst();
        verify(taskRepositoryMock, times(1)).findByDeadlineDateIsNullAndStatusAndAuthor(TaskStatus.PROCESSED, author,
                0, null);
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author)).thenReturn(Mono.empty());

        taskService.getUncompletedTaskCount(author).block();
        verify(taskRepositoryMock, times(1)).countAllByStatusNotAndAuthor(TaskStatus.COMPLETED, author);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        String author = "alice";
        when(taskRepositoryMock.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, 0, null))
                .thenReturn(Flux.empty());

        taskService.getUncompletedTasks(author, Pageable.unpaged()).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, 0, null);
    }

    @Test
    void shouldReturnAllUncompletedTasksWithPagingRestriction() {
        String author = "alice";
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepositoryMock.findByStatusNotAndAuthor(TaskStatus.COMPLETED, author, pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.empty());

        taskService.getUncompletedTasks(author, pageRequest).blockFirst();
        verify(taskRepositoryMock, times(1)).findByStatusNotAndAuthor(TaskStatus.COMPLETED, author,
                pageRequest.getOffset(), pageRequest.getPageSize());
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
    void shouldReturnTaskTags() {
        Task task = Task.builder().id(1L).title("Test task").author("alice").build();
        when(taskRepositoryMock.findByIdAndAuthor(task.getId(), task.getAuthor())).thenReturn(Mono.just(task));

        Tag tag = Tag.builder().id(3L).name("Test tag").author("alice").build();
        when(tagRepositoryMock.findForTaskId(task.getId())).thenReturn(Flux.just(tag));

        Tag result = taskService.getTaskTags(task.getId(), tag.getAuthor()).blockFirst();
        assertNotNull(result);
        assertEquals(tag, result);
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenTaskIsNotFound() {
        when(taskRepositoryMock.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTask(1L, "alice").block());
    }

    @Test
    void shouldThrowExceptionOnTaskTagsGetWhenTaskIsNotFound() {
        when(taskRepositoryMock.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskService.getTaskTags(1L, "alice").blockFirst());
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
