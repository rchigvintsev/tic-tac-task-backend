package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.TaskListRepository;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.model.TaskStatus;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
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
        User user = User.builder().id(1L).build();
        when(taskListRepository.findByCompletedAndUserId(false, user.getId())).thenReturn(Flux.empty());
        taskListService.getUncompletedTaskLists(user).blockFirst();
        verify(taskListRepository, times(1)).findByCompletedAndUserId(false, user.getId());
    }

    @Test
    void shouldReturnTaskListById() {
        User user = User.builder().id(1L).build();
        TaskList taskList = TaskList.builder().id(2L).name("Test task list").userId(user.getId()).build();
        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));

        TaskList result = taskListService.getTaskList(taskList.getId(), user).block();
        assertNotNull(result);
        assertEquals(taskList, result);
    }

    @Test
    void shouldThrowExceptionOnTaskListGetWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        assertThrows(EntityNotFoundException.class, () -> taskListService.getTaskList(2L, user).block());
    }

    @Test
    void shouldCreateTaskList() {
        TaskList taskList = TaskList.builder().name("New task list").userId(1L).build();
        TaskList result = taskListService.createTaskList(taskList).block();
        assertNotNull(result);
        assertEquals(taskList.getName(), result.getName());
        verify(taskListRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionOnTaskListCreateWhenTaskListIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskListService.createTaskList(null));
        assertEquals("Task list must not be null", exception.getMessage());
    }

    @Test
    void shouldUpdateTaskList() {
        TaskList taskList = TaskList.builder().id(1L).name("Test task list").userId(1L).build();
        when(taskListRepository.findByIdAndUserId(taskList.getId(), taskList.getUserId()))
                .thenReturn(Mono.just(taskList));

        TaskList updatedTaskList = taskList.copy();
        updatedTaskList.setId(taskList.getId());
        updatedTaskList.setName("Updated test task list");

        TaskList result = taskListService.updateTaskList(updatedTaskList).block();
        assertNotNull(result);
        assertEquals(updatedTaskList.getName(), result.getName());
    }

    @Test
    void shouldThrowExceptionOnTaskListUpdateWhenTaskListIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskListService.updateTaskList(null));
        assertEquals("Task list must not be null", exception.getMessage());
    }

    @Test
    void shouldCompleteTaskList() {
        User user = User.builder().id(1L).build();
        TaskList taskList = TaskList.builder().id(2L).name("Test task list").userId(user.getId()).build();

        TaskList completedTaskList = taskList.copy();
        completedTaskList.setId(taskList.getId());
        completedTaskList.setCompleted(true);

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId()))
                .thenReturn(Mono.just(taskList));
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskListService.completeTaskList(taskList.getId(), user).block();
        verify(taskListRepository, times(1)).save(completedTaskList);
    }

    @Test
    void shouldCompleteTasksOnTaskListComplete() {
        User user = User.builder().id(1L).build();

        TaskList taskList = TaskList.builder().id(2L).name("Test task list").userId(user.getId()).build();
        Task task = Task.builder().id(3L).title("Test task").userId(user.getId()).taskListId(taskList.getId()).build();

        Task completedTask = task.copy();
        completedTask.setId(task.getId());
        completedTask.setStatus(TaskStatus.COMPLETED);

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.just(task));

        taskListService.completeTaskList(taskList.getId(), user).block();
        verify(taskRepository, times(1)).save(completedTask);
    }

    @Test
    void shouldThrowExceptionOnTaskListCompleteWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        long taskListId = 2L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskListService.completeTaskList(taskListId, user).block());
        assertEquals("Task list with id " + taskListId + " is not found", exception.getMessage());
    }

    @Test
    void shouldDeleteTaskList() {
        User user = User.builder().id(1L).build();
        TaskList taskList = TaskList.builder().id(2L).name("Test task list").userId(user.getId()).build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskListRepository.delete(taskList)).thenReturn(Mono.empty());
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskListService.deleteTaskList(taskList.getId(), user).block();
        verify(taskListRepository, times(1)).delete(taskList);
    }

    @Test
    void shouldDeleteTasksOnTaskListDelete() {
        User user = User.builder().id(1L).build();

        TaskList taskList = TaskList.builder().id(2L).name("Test task list").userId(user.getId()).build();
        Task task = Task.builder().id(3L).title("Test task").userId(user.getId()).taskListId(taskList.getId()).build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskListRepository.delete(taskList)).thenReturn(Mono.empty());
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.just(task));
        when(taskRepository.delete(task)).thenReturn(Mono.empty());

        taskListService.deleteTaskList(taskList.getId(), user).block();
        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void shouldThrowExceptionOnTaskListDeleteWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());

        User user = User.builder().id(1L).build();
        long taskListId = 2L;
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> taskListService.deleteTaskList(taskListId, user).block());
        assertEquals("Task list with id " + taskListId + " is not found", exception.getMessage());
    }

    @Test
    void shouldReturnTasksForTaskListWithPagingRestriction() {
        User user = User.builder().id(1L).build();
        TaskList taskList = TaskList.builder().id(2L).name("Test task list").userId(user.getId()).build();
        Task task = Task.builder()
                .id(2L)
                .taskListId(taskList.getId())
                .userId(user.getId())
                .title("Test task")
                .build();

        when(taskListRepository.findByIdAndUserId(task.getTaskListId(), user.getId())).thenReturn(Mono.just(taskList));

        PageRequest pageRequest = PageRequest.of(3, 50);
        when(taskRepository.findByTaskListIdAndUserId(task.getTaskListId(), user.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.just(task));

        taskListService.getTasks(taskList.getId(), user, pageRequest).blockFirst();
        verify(taskRepository, times(1)).findByTaskListIdAndUserId(taskList.getId(), user.getId(),
                pageRequest.getOffset(), pageRequest.getPageSize());
    }

    @Test
    void shouldThrowExceptionOnTasksGetWhenTaskListIsNotFound() {
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        User user = User.builder().id(1L).build();
        assertThrows(EntityNotFoundException.class,
                () -> taskListService.getTasks(2L, user, Pageable.unpaged()).blockFirst());
    }
}
