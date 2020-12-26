package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskListRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskListServiceTest {
    private TaskListRepository taskListRepository;
    private TaskRepository taskRepository;
    private DefaultTaskListService taskListService;

    @BeforeEach
    void setUp() {
        taskListRepository = mock(TaskListRepository.class);
        when(taskListRepository.save(any()))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, TaskList.class)));
        taskRepository = mock(TaskRepository.class);
        when(taskRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0, Task.class)));
        taskListService = new DefaultTaskListService(taskListRepository, taskRepository);
    }

    @Test
    void shouldReturnAllUncompletedTaskLists() {
        String author = "alice";
        when(taskListRepository.findByCompletedAndAuthor(false, author)).thenReturn(Flux.empty());
        taskListService.getUncompletedTaskLists(author).blockFirst();
        verify(taskListRepository, times(1)).findByCompletedAndAuthor(false, author);
    }

    @Test
    void shouldReturnTaskListById() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));

        TaskList result = taskListService.getTaskList(taskList.getId(), taskList.getAuthor()).block();
        assertNotNull(result);
        assertEquals(taskList, result);
    }

    @Test
    void shouldThrowExceptionOnTaskListGetWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskListService.getTaskList(1L, "alice").block());
    }

    @Test
    void shouldCreateTaskList() {
        TaskList taskList = TaskList.builder().name("New task list").build();
        TaskList result = taskListService.createTaskList(taskList, "alice").block();
        assertNotNull(result);
        assertEquals(taskList.getName(), result.getName());
        verify(taskListRepository, times(1)).save(any());
    }

    @Test
    void shouldSetAuthorFieldOnTaskListCreate() {
        TaskList taskList = TaskList.builder().name("New task list").build();
        String author = "alice";
        TaskList result = taskListService.createTaskList(taskList, author).block();
        assertNotNull(result);
        assertEquals(author, result.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnTaskListCreateWhenTaskListIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskListService.createTaskList(null, null));
        assertEquals("Task list must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskListCreateWhenAuthorIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskListService.createTaskList(TaskList.builder().name("New task list").build(), null));
        assertEquals("Task list author must not be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskListCreateWhenAuthorIsEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskListService.createTaskList(TaskList.builder().name("New task list").build(), ""));
        assertEquals("Task list author must not be null or empty", exception.getMessage());
    }

    @Test
    void shouldUpdateTaskList() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));

        TaskList updatedTaskList = TaskList.builder().name("Updated test task list").build();
        TaskList result = taskListService.updateTaskList(updatedTaskList, taskList.getId(), taskList.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(updatedTaskList.getName(), result.getName());
    }

    @Test
    void shouldSetIdFieldOnTaskListUpdate() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));

        TaskList updatedTaskList = TaskList.builder().name("Updated test task list").build();
        TaskList result = taskListService.updateTaskList(updatedTaskList, taskList.getId(), taskList.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(taskList.getId(), result.getId());
    }

    @Test
    void shouldSetAuthorFieldOnTaskListUpdate() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));

        TaskList updatedTaskList = TaskList.builder().name("Updated test task list").build();
        TaskList result = taskListService.updateTaskList(updatedTaskList, taskList.getId(), taskList.getAuthor())
                .block();
        assertNotNull(result);
        assertEquals(taskList.getAuthor(), result.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnTaskListUpdateWhenTaskListIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskListService.updateTaskList(null, null, null));
        assertEquals("Task list must not be null", exception.getMessage());
    }

    @Test
    void shouldCompleteTaskList() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        TaskList completedTaskList = taskList.copy();
        completedTaskList.setId(taskList.getId());
        completedTaskList.setCompleted(true);

        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));
        when(taskRepository.findByTaskListIdAndAuthor(taskList.getId(), taskList.getAuthor(), 0, null))
                .thenReturn(Flux.empty());

        taskListService.completeTaskList(taskList.getId(), taskList.getAuthor()).block();
        verify(taskListRepository, times(1)).save(completedTaskList);
    }

    @Test
    void shouldCompleteTasksOnTaskListComplete() {
        String author = "alice";

        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author(author).build();
        Task task = Task.builder().id(2L).title("Test task").author(author).taskListId(taskList.getId()).build();
        Task completedTask = task.copy();
        completedTask.setId(task.getId());
        completedTask.setStatus(TaskStatus.COMPLETED);

        when(taskListRepository.findByIdAndAuthor(taskList.getId(), author)).thenReturn(Mono.just(taskList));
        when(taskRepository.findByTaskListIdAndAuthor(taskList.getId(), author, 0, null)).thenReturn(Flux.just(task));

        taskListService.completeTaskList(taskList.getId(), taskList.getAuthor()).block();
        verify(taskRepository, times(1)).save(completedTask);
    }

    @Test
    void shouldThrowExceptionOnTaskListCompleteWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long taskListId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskListService.completeTaskList(taskListId, "alice").block());
        assertEquals("Task list with id " + taskListId + " is not found", exception.getMessage());
    }

    @Test
    void shouldDeleteTaskList() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();

        when(taskListRepository.findByIdAndAuthor(taskList.getId(), taskList.getAuthor()))
                .thenReturn(Mono.just(taskList));
        when(taskListRepository.deleteByIdAndAuthor(taskList.getId(), taskList.getAuthor())).thenReturn(Mono.empty());

        taskListService.deleteTaskList(taskList.getId(), taskList.getAuthor()).block();
        verify(taskListRepository, times(1)).deleteByIdAndAuthor(taskList.getId(), taskList.getAuthor());
    }

    @Test
    void shouldThrowExceptionOnTaskListDeleteWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        long taskListId = 1L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskListService.deleteTaskList(taskListId, "alice").block());
        assertEquals("Task list with id " + taskListId + " is not found", exception.getMessage());
    }

    @Test
    void shouldReturnTasksForTaskListWithPagingRestriction() {
        PageRequest pageRequest = PageRequest.of(3, 50);

        TaskList taskList = TaskList.builder().id(1L).name("Test task list").author("alice").build();
        Task task = Task.builder()
                .id(2L)
                .taskListId(taskList.getId())
                .author(taskList.getAuthor())
                .title("Test task")
                .build();
        when(taskListRepository.findByIdAndAuthor(task.getTaskListId(), task.getAuthor()))
                .thenReturn(Mono.just(taskList));
        when(taskRepository.findByTaskListIdAndAuthor(task.getTaskListId(), task.getAuthor(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.just(task));

        taskListService.getTasks(taskList.getId(), taskList.getAuthor(), pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByTaskListIdAndAuthor(taskList.getId(), taskList.getAuthor(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldThrowExceptionOnTasksGetWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndAuthor(anyLong(), anyString())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class,
                () -> taskListService.getTasks(1L, "alice", Pageable.unpaged()).blockFirst());
    }
}
