package org.briarheart.tictactask.task;

import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.comment.TaskCommentRepository;
import org.briarheart.tictactask.task.list.TaskList;
import org.briarheart.tictactask.task.list.TaskListRepository;
import org.briarheart.tictactask.task.recurrence.DailyTaskRecurrenceStrategy;
import org.briarheart.tictactask.task.tag.TagRepository;
import org.briarheart.tictactask.task.tag.TaskTag;
import org.briarheart.tictactask.task.tag.TaskTagRelation;
import org.briarheart.tictactask.task.tag.TaskTagRelationRepository;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultTaskServiceTest {
    private TaskRepository taskRepository;
    private TaskTagRelationRepository taskTagRelationRepository;
    private TagRepository tagRepository;
    private TaskListRepository taskListRepository;
    private TaskCommentRepository taskCommentRepository;

    private DefaultTaskService taskService;
    private LocalDateTime currentTime;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        taskTagRelationRepository = mock(TaskTagRelationRepository.class);
        tagRepository = mock(TagRepository.class);
        taskListRepository = mock(TaskListRepository.class);
        taskCommentRepository = mock(TaskCommentRepository.class);

        currentTime = LocalDateTime.now(ZoneOffset.UTC);
        taskService = new DefaultTaskService(taskRepository, taskTagRelationRepository, tagRepository,
                taskListRepository, taskCommentRepository) {
            @Override
            protected LocalDateTime getCurrentTime() {
                return currentTime;
            }
        };
    }

    @Test
    void shouldReturnNumberOfAllTasks() {
        GetTasksRequest request = new GetTasksRequest();
        when(taskRepository.count(request, TestUsers.JOHN_DOE)).thenReturn(Mono.just(1L));
        assertEquals(1L, taskService.getTaskCount(request, TestUsers.JOHN_DOE).block());
    }

    @Test
    void shouldReturnAllTasks() {
        GetTasksRequest request = new GetTasksRequest();
        Task task = Task.builder().id(2L).userId(TestUsers.JOHN_DOE.getId()).title("Test task").build();
        when(taskRepository.find(request, TestUsers.JOHN_DOE, Pageable.unpaged())).thenReturn(Flux.just(task));

        Task result = taskService.getTasks(request, TestUsers.JOHN_DOE, Pageable.unpaged()).blockFirst();
        assertEquals(task, result);
    }

    @Test
    void shouldReturnTaskById() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));

        Task result = taskService.getTask(task.getId(), user).block();
        assertEquals(task, result);
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.getTask(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskGetWhenTaskIsNotFound() {
        User user = createActiveUser();
        when(taskRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        long taskId = 2L;
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.getTask(taskId, user).block());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldCreateTask() {
        long taskId = 2L;
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> {
            Task t = new Task(args.getArgument(0));
            t.setId(taskId);
            return Mono.just(t);
        });

        Task task = Task.builder().userId(1L).status(TaskStatus.PROCESSED).title("New task").build();

        Task expectedResult = new Task(task);
        expectedResult.setId(taskId);
        expectedResult.setCreatedAt(currentTime);

        Task result = taskService.createTask(task).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToSetTaskListIdFieldOnTaskCreate() {
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task task = Task.builder().userId(1L).taskListId(2L).status(TaskStatus.PROCESSED).title("New task").build();

        Task expectedResult = new Task(task);
        expectedResult.setTaskListId(null);
        expectedResult.setCreatedAt(currentTime);

        Task result = taskService.createTask(task).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldSetTaskStatusToUnprocessedOnTaskCreateWhenStatusIsNotProvided() {
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task task = Task.builder().userId(1L).title("New task").build();

        Task expectedResult = new Task(task);
        expectedResult.setStatus(TaskStatus.UNPROCESSED);
        expectedResult.setCreatedAt(currentTime);

        Task result = taskService.createTask(task).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnTaskCreateWhenTaskIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> taskService.createTask(null));
        assertEquals("Task must not be null", exception.getMessage());
    }

    @Test
    void shouldUpdateTask() {
        Task task = Task.builder()
                .id(1L)
                .userId(2L)
                .taskListId(3L)
                .title("Test task")
                .createdAt(currentTime)
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task updatedTask = new Task(task);
        updatedTask.setTitle("Updated test task");

        Task result = taskService.updateTask(updatedTask).block();
        assertEquals(updatedTask, result);
    }

    // Task should be added to task list using separate service org.briarheart.tictactask.task.list.TaskListService
    @Test
    void shouldNotAllowToChangeTaskListIdFieldOnTaskUpdate() {
        Task task = Task.builder()
                .id(1L)
                .userId(2L)
                .taskListId(3L)
                .title("Test task")
                .createdAt(currentTime)
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task updatedTask = new Task(task);
        updatedTask.setTaskListId(4L);

        Task result = taskService.updateTask(updatedTask).block();
        assertEquals(task, result);
    }

    @Test
    void shouldNotAllowToChangeCreatedAtFieldOnTaskUpdate() {
        Task task = Task.builder()
                .id(1L)
                .userId(2L)
                .taskListId(3L)
                .title("Test task")
                .createdAt(currentTime)
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task updatedTask = new Task(task);
        updatedTask.setCreatedAt(currentTime.minus(1, ChronoUnit.HOURS));

        Task result = taskService.updateTask(updatedTask).block();
        assertEquals(task, result);
    }

    @Test
    void shouldNotAllowToMarkTaskCompletedOnTaskUpdate() {
        Task task = Task.builder()
                .id(1L)
                .userId(2L)
                .taskListId(3L)
                .title("Test task")
                .createdAt(currentTime)
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task updatedTask = new Task(task);
        updatedTask.setStatus(TaskStatus.COMPLETED);

        Task result = taskService.updateTask(updatedTask).block();
        assertNotNull(result);
        assertSame(TaskStatus.UNPROCESSED, result.getStatus());
    }

    @Test
    void shouldMarkTaskAsProcessedOnTaskUpdateWhenDeadlineDateIsNotNull() {
        Task task = Task.builder().id(1L).userId(2L).title("Test task").status(TaskStatus.UNPROCESSED).build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task updatedTask = new Task(task);
        updatedTask.setDeadline(currentTime.plus(3, ChronoUnit.DAYS));

        Task result = taskService.updateTask(updatedTask).block();
        assertNotNull(result);
        assertSame(TaskStatus.PROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskUpdateWhenTaskIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(null));
        assertEquals("Task must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskUpdateWhenTaskIsNotFound() {
        Task task = Task.builder().id(1L).userId(2L).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.updateTask(task).block());
        assertEquals("Task with id " + task.getId() + " is not found", e.getMessage());
    }

    @Test
    void shouldCompleteTask() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        taskService.completeTask(task.getId(), user).block();
        assertSame(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    void shouldRescheduleTaskOnCompleteWhenTaskRecurrenceIsEnabled() {
        User user = createActiveUser();
        LocalDateTime taskDeadline = LocalDateTime.now(ZoneOffset.UTC);
        Task task = Task.builder()
                .id(2L)
                .userId(user.getId())
                .title("Test task")
                .deadline(taskDeadline)
                .recurrenceStrategy(new DailyTaskRecurrenceStrategy())
                .build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        taskService.completeTask(task.getId(), user).block();
        assertSame(TaskStatus.UNPROCESSED, task.getStatus());
        assertTrue(task.getDeadline().isAfter(taskDeadline));
    }

    @Test
    void shouldUpdateCompletedAtAttributeOnTaskComplete() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        taskService.completeTask(task.getId(), user).block();
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void shouldThrowExceptionOnTaskCompleteWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.completeTask(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskCompleteWhenTaskIsNotFound() {
        User user = createActiveUser();
        long taskId = 2L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.completeTask(taskId, user).block());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldRestoreTask() {
        User user = createActiveUser();
        Task task = Task.builder()
                .id(2L)
                .userId(user.getId())
                .title("Test task")
                .previousStatus(TaskStatus.PROCESSED)
                .status(TaskStatus.COMPLETED)
                .build();
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task result = taskService.restoreTask(task.getId(), user).block();
        assertNotNull(result);
        assertSame(TaskStatus.PROCESSED, result.getStatus());
    }

    @Test
    void shouldRestoreTaskFromTaskList() {
        User user = createActiveUser();
        TaskList taskList = TaskList.builder()
                .id(2L)
                .userId(user.getId())
                .name("Test task list")
                .completed(false)
                .build();
        Task task = Task.builder()
                .id(3L)
                .userId(user.getId())
                .taskListId(taskList.getId())
                .title("Test task")
                .previousStatus(TaskStatus.PROCESSED)
                .status(TaskStatus.COMPLETED)
                .build();
        when(taskListRepository.findById(taskList.getId())).thenReturn(Mono.just(taskList));
        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        Task result = taskService.restoreTask(task.getId(), user).block();
        assertNotNull(result);
        assertSame(TaskStatus.PROCESSED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionOnTaskRestoreWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.restoreTask(1L, null));
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskRestoreWhenTaskIsNotFound() {
        User user = createActiveUser();
        long taskId = 2L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.restoreTask(taskId, user).block());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldDeleteTask() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskRepository.delete(task)).thenReturn(Mono.just(true).then());

        taskService.deleteTask(task.getId(), user).block();
        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    void shouldThrowExceptionOnTaskDeleteWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.deleteTask(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTaskDeleteWhenTaskIsNotFound() {
        User user = createActiveUser();
        long taskId = 2L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.deleteTask(taskId, user).block());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldReturnAllTagsForTask() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        TaskTag tag = TaskTag.builder().id(3L).userId(user.getId()).name("Test tag").build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.findByTaskId(task.getId()))
                .thenReturn(Flux.just(new TaskTagRelation(task.getId(), tag.getId())));
        when(tagRepository.findByIdIn(Set.of(tag.getId()))).thenReturn(Flux.just(tag));

        TaskTag result = taskService.getTags(task.getId(), user).blockFirst();
        assertEquals(tag, result);
    }

    @Test
    void shouldThrowExceptionOnTagsGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.getTags(1L, null).blockFirst());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagsGetWhenTaskIsNotFound() {
        User user = createActiveUser();
        long taskId = 2L;

        when(taskRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.getTags(taskId, user).blockFirst());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldAssignTagToTask() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        TaskTag tag = TaskTag.builder().id(3L).userId(user.getId()).name("Test tag").build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(taskTagRelationRepository.findByTaskIdAndTagId(task.getId(), tag.getId())).thenReturn(Mono.empty());
        when(taskTagRelationRepository.create(task.getId(), tag.getId()))
                .thenAnswer(args -> Mono.just(new TaskTagRelation(task.getId(), tag.getId())));

        taskService.assignTag(task.getId(), tag.getId(), user).block();
        verify(taskTagRelationRepository, times(1)).create(task.getId(), tag.getId());
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.assignTag(1L, 2L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenTaskIsNotFound() {
        User user = createActiveUser();
        TaskTag tag = TaskTag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        long taskId = 3L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        when(tagRepository.findByIdAndUserId(tag.getId(), user.getId())).thenReturn(Mono.just(tag));
        when(taskTagRelationRepository.create(anyLong(), anyLong())).thenAnswer(args -> Mono.empty());

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.assignTag(taskId, tag.getId(), user).block());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagAssignWhenTagIsNotFound() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        Long tagId = 3L;

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(tagRepository.findByIdAndUserId(tagId, user.getId())).thenReturn(Mono.empty());
        when(taskTagRelationRepository.create(anyLong(), anyLong())).thenAnswer(args -> Mono.empty());

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.assignTag(task.getId(), tagId, user).block());
        assertEquals("Tag with id " + tagId + " is not found", e.getMessage());
    }

    @Test
    void shouldRemoveTagFromTask() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).title("Test task").userId(user.getId()).build();
        TaskTag tag = TaskTag.builder().id(3L).name("Test tag").userId(user.getId()).build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskTagRelationRepository.deleteByTaskIdAndTagId(task.getId(), tag.getId()))
                .thenReturn(Mono.empty().then());

        taskService.removeTag(task.getId(), tag.getId(), user).block();
        verify(taskTagRelationRepository, times(1)).deleteByTaskIdAndTagId(task.getId(), tag.getId());
    }

    @Test
    void shouldThrowExceptionOnTagRemoveWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.removeTag(1L, 2L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnTagRemoveWhenTaskIsNotFound() {
        User user = createActiveUser();
        Long taskId = 2L;
        Long tagId = 3L;

        when(taskRepository.findByIdAndUserId(taskId, user.getId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.removeTag(taskId, tagId, user).block());
        assertEquals("Task with id " + taskId + " is not found", e.getMessage());
    }

    @Test
    void shouldReturnAllCommentsForTask() {
        User user = createActiveUser();
        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        TaskComment comment = TaskComment.builder()
                .id(3L)
                .userId(user.getId())
                .taskId(task.getId())
                .commentText("Test task comment")
                .build();

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), 0, null))
                .thenReturn(Flux.just(comment));

        TaskComment result = taskService.getComments(task.getId(), user, Pageable.unpaged()).blockFirst();
        assertEquals(comment, result);
    }

    @Test
    void shouldReturnCommentsForTaskWithPagingRestriction() {
        User user = createActiveUser();
        Task task = Task.builder().id(1L).userId(user.getId()).title("Test task").build();
        TaskComment comment = TaskComment.builder()
                .id(3L)
                .userId(user.getId())
                .taskId(task.getId())
                .commentText("Test task comment")
                .build();
        PageRequest pageRequest = PageRequest.of(3, 50);

        when(taskRepository.findByIdAndUserId(task.getId(), user.getId())).thenReturn(Mono.just(task));
        when(taskCommentRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), pageRequest.getOffset(),
                pageRequest.getPageSize())).thenReturn(Flux.just(comment));

        TaskComment result = taskService.getComments(task.getId(), user, pageRequest).blockFirst();
        assertEquals(comment, result);
    }

    @Test
    void shouldThrowExceptionOnCommentsGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> taskService.getComments(1L, null, Pageable.unpaged()).blockFirst());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCommentsGetWhenTaskIsNotFound() {
        User user = createActiveUser();
        when(taskRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        assertThrows(EntityNotFoundException.class,
                () -> taskService.getComments(1L, user, Pageable.unpaged()).blockFirst());
    }

    @Test
    void shouldAddCommentToTask() {
        Task task = Task.builder().id(2L).userId(1L).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));

        long commentId = 3L;

        when(taskCommentRepository.save(any())).thenAnswer(args -> {
            TaskComment comment = new TaskComment(args.getArgument(0));
            comment.setId(commentId);
            return Mono.just(comment);
        });

        TaskComment newComment = TaskComment.builder()
                .commentText("New comment")
                .userId(task.getUserId())
                .taskId(task.getId())
                .build();

        TaskComment expectedResult = new TaskComment(newComment);
        expectedResult.setId(commentId);
        expectedResult.setCreatedAt(currentTime);

        TaskComment result = taskService.addComment(newComment).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToSetUpdatedAtFieldOnCommentAdd() {
        Task task = Task.builder().id(2L).userId(1L).title("Test task").build();
        when(taskRepository.findByIdAndUserId(task.getId(), task.getUserId())).thenReturn(Mono.just(task));

        when(taskCommentRepository.save(any())).thenAnswer(args -> Mono.just(new TaskComment(args.getArgument(0))));

        TaskComment newComment = TaskComment.builder()
                .commentText("New comment")
                .userId(task.getUserId())
                .taskId(task.getId())
                .updatedAt(currentTime)
                .build();

        TaskComment expectedResult = new TaskComment(newComment);
        expectedResult.setCreatedAt(currentTime);
        expectedResult.setUpdatedAt(null);

        TaskComment result = taskService.addComment(newComment).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnCommentAddWhenCommentIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> taskService.addComment(null));
        assertEquals("Task comment must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnCommentAddWhenTaskIsNotFound() {
        TaskComment comment = TaskComment.builder()
                .commentText("New comment")
                .userId(1L)
                .taskId(2L)
                .build();
        when(taskRepository.findByIdAndUserId(comment.getTaskId(), comment.getUserId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> taskService.addComment(comment).block());
        assertEquals("Task with id " + comment.getTaskId() + " is not found", e.getMessage());
    }

    private User createActiveUser() {
        return User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
    }
}
