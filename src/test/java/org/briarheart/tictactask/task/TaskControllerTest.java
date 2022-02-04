package org.briarheart.tictactask.task;

import org.briarheart.tictactask.config.PermitAllSecurityConfig;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.TaskController.CreateTaskRequest;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.task.TaskController.UpdateTaskRequest;
import org.briarheart.tictactask.task.comment.TaskComment;
import org.briarheart.tictactask.task.comment.TaskCommentController.CreateTaskCommentRequest;
import org.briarheart.tictactask.task.comment.TaskCommentController.TaskCommentResponse;
import org.briarheart.tictactask.task.tag.TaskTag;
import org.briarheart.tictactask.task.tag.TaskTagController.TaskTagResponse;
import org.briarheart.tictactask.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@WebFluxTest(TaskController.class)
@Import(PermitAllSecurityConfig.class)
@ActiveProfiles("test")
class TaskControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TaskService taskService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void shouldReturnAllUnprocessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED));

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        when(taskService.getTasks(eq(request), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldReturnNumberOfAllUnprocessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED));

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getTaskCount(request, user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/count?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .id(2L)
                .userId(user.getId())
                .title("Test task")
                .status(TaskStatus.PROCESSED)
                .build();

        when(taskService.getTasks(eq(request), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldReturnNumberOfAllProcessedTasks() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getTaskCount(request, user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/count?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnProcessedTasksWithoutDeadline() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(null);
        request.setDeadlineTo(null);

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .id(2L)
                .userId(user.getId())
                .title("Test task")
                .status(TaskStatus.PROCESSED)
                .build();

        when(taskService.getTasks(eq(request), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithoutDeadline() {
        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(null);
        request.setDeadlineTo(null);

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getTaskCount(request, user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/count?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnProcessedTasksWithDeadline() {
        String deadlineFrom = "2020-01-01T00:00";
        String deadlineTo = "2020-01-31T23:59";

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseIsoDateTime(deadlineFrom));
        request.setDeadlineTo(parseIsoDateTime(deadlineTo));

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder()
                .id(2L)
                .userId(user.getId())
                .title("Test task")
                .status(TaskStatus.PROCESSED)
                .deadline(parseIsoDateTime("2020-01-10T00:00:00"))
                .build();

        when(taskService.getTasks(eq(request), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldReturnNumberOfProcessedTasksWithDeadline() {
        String deadlineFrom = "2020-01-01T00:00";
        String deadlineTo = "2020-01-31T23:59";

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.PROCESSED));
        request.setDeadlineFrom(parseIsoDateTime(deadlineFrom));
        request.setDeadlineTo(parseIsoDateTime(deadlineTo));

        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.getTaskCount(request, user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/count?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllUncompletedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        when(taskService.getTasks(eq(request), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldReturnNumberOfAllUncompletedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.UNPROCESSED, TaskStatus.PROCESSED));

        when(taskService.getTaskCount(request, user)).thenReturn(Mono.just(1L));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/count?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(1L);
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        GetTasksRequest request = new GetTasksRequest();
        request.setStatuses(Set.of(TaskStatus.COMPLETED));

        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").status(TaskStatus.COMPLETED).build();
        when(taskService.getTasks(eq(request), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks?" + requestToQueryString(request))
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldReturnTaskById() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();
        when(taskService.getTask(task.getId(), user)).thenReturn(Mono.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/" + task.getId())
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse.class).isEqualTo(new TaskResponse(task));
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTaskIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Task is not found";
        when(taskService.getTask(anyLong(), any(User.class)))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/api/v1/tasks/2").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldCreateTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        when(taskService.createTask(any(Task.class))).thenAnswer(args -> {
            Task t = new Task(args.getArgument(0));
            t.setId(taskId);
            return Mono.just(t);
        });

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("New task");

        TaskResponse expectedResult = new TaskResponse(createRequest.toTask());
        expectedResult.setId(taskId);

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/v1/tasks/" + taskId)
                .expectBody(TaskResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskRequest createRequest = new CreateTaskRequest();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle(" ");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTaskCreationWhenTitleIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("L" + "o".repeat(247) + "ng title");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value length must not be greater than 255");
    }

    @Test
    void shouldRejectTaskCreationWhenDescriptionIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("Test title");
        createRequest.setDescription("L" + "o".repeat(9986) + "ng description");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value length must not be greater than 10000");
    }

    @Test
    void shouldRejectTaskCreationWhenDeadlineIsInPast() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskRequest createRequest = new CreateTaskRequest();
        createRequest.setTitle("Test title");
        createRequest.setDeadline(LocalDateTime.now().minus(3, ChronoUnit.DAYS));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be in past");
    }

    @Test
    void shouldUpdateTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskService.updateTask(any(Task.class))).thenAnswer(args -> Mono.just(new Task(args.getArgument(0))));

        long taskId = 2L;

        UpdateTaskRequest updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Updated test task");

        TaskResponse expectedResult = new TaskResponse(updateRequest.toTask());
        expectedResult.setId(taskId);

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .put().uri("/api/v1/tasks/" + taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldCompleteTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        when(taskService.completeTask(taskId, user))
                .thenReturn(Mono.just(Task.builder().id(taskId).status(TaskStatus.COMPLETED).build()));

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .put().uri("/api/v1/tasks/completed/" + taskId)
                .exchange()

                .expectStatus().isOk();
        verify(taskService, times(1)).completeTask(taskId, user);
    }

    @Test
    void shouldRestoreTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task restoredTask = Task.builder()
                .id(2L)
                .userId(user.getId())
                .title("Restored test task")
                .status(TaskStatus.PROCESSED)
                .build();
        when(taskService.restoreTask(restoredTask.getId(), user)).thenReturn(Mono.just(restoredTask));

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .delete().uri("/api/v1/tasks/completed/" + restoredTask.getId())
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse.class).isEqualTo(new TaskResponse(restoredTask));
    }

    @Test
    void shouldDeleteTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        when(taskService.deleteTask(taskId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .delete().uri("/api/v1/tasks/" + taskId)
                .exchange()

                .expectStatus().isNoContent();
        verify(taskService, times(1)).deleteTask(taskId, user);
    }

    @Test
    void shouldReturnTagsForTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;

        TaskTag tag = TaskTag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        Mockito.when(taskService.getTags(taskId, user)).thenReturn(Flux.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/" + taskId + "/tags")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskTagResponse[].class).isEqualTo(new TaskTagResponse[]{new TaskTagResponse(tag)});
    }

    @Test
    void shouldAssignTagToTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;
        long tagId = 3L;

        when(taskService.assignTag(taskId, tagId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/tasks/" + taskId + "/tags/" + tagId)
                .exchange()

                .expectStatus().isNoContent();
        verify(taskService, times(1)).assignTag(taskId, tagId, user);
    }

    @Test
    void shouldRemoveTagFromTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;
        long tagId = 3L;

        when(taskService.removeTag(taskId, tagId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .delete().uri("/api/v1/tasks/" + taskId + "/tags/" + tagId)
                .exchange()

                .expectStatus().isNoContent();
        verify(taskService, times(1)).removeTag(taskId, tagId, user);
    }

    @Test
    void shouldReturnCommentsForTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        TaskComment comment = TaskComment.builder()
                .id(2L)
                .userId(user.getId())
                .taskId(3L)
                .commentText("Test comment")
                .build();

        Mockito.when(taskService.getComments(
                comment.getTaskId(),
                user,
                PageRequest.of(0, 20)
        )).thenReturn(Flux.just(comment));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tasks/" + comment.getTaskId() + "/comments")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskCommentResponse[].class)
                    .isEqualTo(new TaskCommentResponse[]{new TaskCommentResponse(comment)});
    }

    @Test
    void shouldAddCommentToTask() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskId = 2L;
        long commentId = 3L;

        when(taskService.addComment(any(TaskComment.class))).thenAnswer(args -> {
            TaskComment c = new TaskComment(args.getArgument(0));
            c.setId(commentId);
            return Mono.just(c);
        });

        CreateTaskCommentRequest createRequest = new CreateTaskCommentRequest();
        createRequest.setCommentText("Test comment");

        TaskCommentResponse expectedResult = new TaskCommentResponse(createRequest.toTaskComment());
        expectedResult.setId(commentId);
        expectedResult.setTaskId(taskId);

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks/" + taskId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isCreated()
                .expectBody(TaskCommentResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectCommentAddingWhenCommentTextIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskCommentRequest createRequest = new CreateTaskCommentRequest();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks/2/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectCommentAddingWhenCommentTextIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskCommentRequest createRequest = new CreateTaskCommentRequest();
        createRequest.setCommentText(" ");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks/2/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectCommentAddingWhenCommentTextIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskCommentRequest createRequest = new CreateTaskCommentRequest();
        createRequest.setCommentText("L" + "o".repeat(9993) + "ng text");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tasks/2/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value length must not be greater than 10000");
    }

    private Authentication createAuthentication(User user) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(user.getEmail());
        when(authenticationMock.getPrincipal()).thenReturn(user);
        return authenticationMock;
    }

    private String requestToQueryString(GetTasksRequest request) {
        String result = "statuses=" + request.getStatuses().stream()
                .map(Enum::toString)
                .collect(Collectors.joining(","));
        if (request.isDeadlineFromDirty()) {
            result += "&deadlineFrom=" + Objects.toString(request.getDeadlineFrom(), "");
        }
        if (request.isDeadlineToDirty()) {
            result += "&deadlineTo=" + Objects.toString(request.getDeadlineTo(), "");
        }
        return result;
    }

    private LocalDateTime parseIsoDateTime(String dateTime) {
        return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
    }
}
