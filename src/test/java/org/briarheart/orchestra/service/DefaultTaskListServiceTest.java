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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        taskRepository = mock(TaskRepository.class);
        taskListService = new DefaultTaskListService(taskListRepository, taskRepository);
    }

    @Test
    void shouldReturnAllUncompletedTaskLists() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        when(taskListRepository.findByCompletedAndUserId(false, user.getId())).thenReturn(Flux.just(taskList));

        TaskList result = taskListService.getUncompletedTaskLists(user).blockFirst();
        assertEquals(taskList, result);
    }

    @Test
    void shouldThrowExceptionOnUncompletedTaskListsGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.getUncompletedTaskLists(null).blockFirst());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldReturnTaskListById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));

        TaskList result = taskListService.getTaskList(taskList.getId(), user).block();
        assertEquals(taskList, result);
    }

    @Test
    void shouldThrowExceptionOnTaskListGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.getTaskList(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskListGetWhenTaskListIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class, () -> taskListService.getTaskList(2L, user).block());
    }

    @Test
    void shouldCreateTaskList() {
        long taskListId = 2L;
        when(taskListRepository.save(any(TaskList.class))).thenAnswer(args -> {
            TaskList l = args.getArgument(0);
            if (l.getId() == null) {
                l.setId(taskListId);
            }
            return Mono.just(l);
        });

        TaskList taskList = TaskList.builder().id(-1L).userId(1L).name("New task list").build();
        TaskList expectedResult = new TaskList(taskList);
        expectedResult.setId(taskListId);

        TaskList result = taskListService.createTaskList(taskList).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnTaskListCreateWhenTaskListIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.createTaskList(null));
        assertEquals("Task list must not be null", e.getMessage());
    }

    @Test
    void shouldUpdateTaskList() {
        TaskList taskList = TaskList.builder().id(2L).userId(1L).name("Test task list").build();
        when(taskListRepository.findByIdAndUserId(taskList.getId(), taskList.getUserId()))
                .thenReturn(Mono.just(taskList));
        when(taskListRepository.save(any(TaskList.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        TaskList updatedTaskList = new TaskList(taskList);
        updatedTaskList.setName("Updated test task list");

        TaskList result = taskListService.updateTaskList(updatedTaskList).block();
        assertEquals(updatedTaskList, result);
    }

    @Test
    void shouldThrowExceptionOnTaskListUpdateWhenTaskListIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.updateTaskList(null));
        assertEquals("Task list must not be null", e.getMessage());
    }

    @Test
    void shouldCompleteTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskListRepository.save(any(TaskList.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.empty());

        TaskList completedTaskList = new TaskList(taskList);
        completedTaskList.setCompleted(true);

        taskListService.completeTaskList(taskList.getId(), user).block();
        verify(taskListRepository, times(1)).save(completedTaskList);
    }

    @Test
    void shouldCompleteTasksOnTaskListComplete() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        Task task = Task.builder().id(3L).userId(user.getId()).taskListId(taskList.getId()).title("Test task").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskListRepository.save(any(TaskList.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        Task completedTask = new Task(task);
        completedTask.setStatus(TaskStatus.COMPLETED);

        taskListService.completeTaskList(taskList.getId(), user).block();
        verify(taskRepository, times(1)).save(completedTask);
    }

    @Test
    void shouldThrowExceptionOnTaskListCompleteWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.completeTaskList(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskListCompleteWhenTaskListIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        long taskListId = 2L;
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskListService.completeTaskList(taskListId, user).block());
        assertEquals("Task list with id " + taskListId + " is not found", e.getMessage());
    }

    @Test
    void shouldDeleteTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskListRepository.delete(taskList)).thenReturn(Mono.just(true).then());
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.empty());

        taskListService.deleteTaskList(taskList.getId(), user).block();
        verify(taskListRepository, times(1)).delete(taskList);
    }

    @Test
    void shouldDeleteTasksOnTaskListDelete() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        Task task = Task.builder().id(3L).userId(user.getId()).taskListId(taskList.getId()).title("Test task").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskListRepository.delete(taskList)).thenReturn(Mono.just(true).then());
        when(taskRepository.findByTaskListIdAndUserId(taskList.getId(), user.getId(), 0, null))
                .thenReturn(Flux.just(task));
        when(taskRepository.delete(task)).thenReturn(Mono.empty());

        taskListService.deleteTaskList(taskList.getId(), user).block();
        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void shouldThrowExceptionOnTaskListDeleteWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.deleteTaskList(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskListDeleteWhenTaskListIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        long taskListId = 2L;
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskListService.deleteTaskList(taskListId, user).block());
        assertEquals("Task list with id " + taskListId + " is not found", e.getMessage());
    }

    @Test
    void shouldReturnTasksForTaskListWithPagingRestriction() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        Task task = Task.builder().id(3L).userId(user.getId()).taskListId(taskList.getId()).title("Test task").build();

        when(taskListRepository.findByIdAndUserId(task.getTaskListId(), user.getId())).thenReturn(Mono.just(taskList));
        PageRequest pageRequest = PageRequest.of(3, 50);
        when(taskRepository.findByTaskListIdAndUserId(task.getTaskListId(), user.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.just(task));

        Task result = taskListService.getTasks(taskList.getId(), user, pageRequest).blockFirst();
        assertEquals(task, result);
    }

    @Test
    void shouldThrowExceptionOnTasksGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.getTasks(1L, null, Pageable.unpaged()).blockFirst());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTasksGetWhenTaskListIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        long taskListId = 2L;
        when(taskListRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskListService.getTasks(taskListId, user, Pageable.unpaged()).blockFirst());
        assertEquals("Task list with id " + taskListId + " is not found", e.getMessage());
    }

    @Test
    void shouldAddTaskToTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        Task task = Task.builder().id(3L).userId(user.getId()).title("Test task").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        taskListService.addTask(taskList.getId(), task.getId(), user).block();

        Task addedTask = new Task(task);
        addedTask.setTaskListId(taskList.getId());
        verify(taskRepository, times(1)).save(addedTask);
    }

    @Test
    void shouldThrowExceptionOnTaskAddWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskListService.addTask(1L, 2L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskAddWhenTaskListIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        Task task = Task.builder().id(3L).userId(user.getId()).title("Test task").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.empty());
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskListService.addTask(taskList.getId(), task.getId(), user).block());
        assertEquals("Task list with id " + taskList.getId() + " is not found", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskAddWhenTaskIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        Task task = Task.builder().id(3L).userId(user.getId()).title("Test task").build();

        when(taskListRepository.findByIdAndUserId(taskList.getId(), user.getId())).thenReturn(Mono.just(taskList));
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.empty());

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskListService.addTask(taskList.getId(), task.getId(), user).block());
        assertEquals("Task with id " + task.getId() + " is not found", e.getMessage());
    }
}
