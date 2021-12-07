package org.briarheart.tictactask.task.list;

import org.briarheart.tictactask.config.PermitAllSecurityConfig;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.Task;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.task.list.TaskListController.CreateTaskListRequest;
import org.briarheart.tictactask.task.list.TaskListController.TaskListResponse;
import org.briarheart.tictactask.task.list.TaskListController.UpdateTaskListRequest;
import org.briarheart.tictactask.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TaskListController.class)
@Import(PermitAllSecurityConfig.class)
class TaskListControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TaskListService taskListService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void shouldReturnAllUncompletedTaskLists() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        when(taskListService.getUncompletedTaskLists(user)).thenReturn(Flux.just(taskList));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/task-lists/uncompleted")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskListResponse[].class).isEqualTo(new TaskListResponse[] {new TaskListResponse(taskList)});
    }

    @Test
    void shouldReturnTaskListById() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        when(taskListService.getTaskList(taskList.getId(), user)).thenReturn(Mono.just(taskList));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1//task-lists/" + taskList.getId())
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskListResponse.class).isEqualTo(new TaskListResponse(taskList));
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTaskListIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Task list is not found";
        when(taskListService.getTaskList(anyLong(), any(User.class)))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/task-lists/2")
                .exchange()

                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldCreateTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        when(taskListService.createTaskList(any(TaskList.class))).thenAnswer(args -> {
            TaskList l = args.getArgument(0);
            l.setId(taskListId);
            return Mono.just(l);
        });

        CreateTaskListRequest createRequest = new CreateTaskListRequest();
        createRequest.setName("New task list");

        TaskListResponse expectedResult = new TaskListResponse(createRequest.toTaskList());
        expectedResult.setId(taskListId);

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/v1/task-lists/" + taskListId)
                .expectBody(TaskListResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectTaskListCreationWhenNameIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskListRequest createRequest = new CreateTaskListRequest();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTaskListCreationWhenNameIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTaskListRequest createRequest = new CreateTaskListRequest();
        createRequest.setName(" ");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldUpdateTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskListService.updateTaskList(any(TaskList.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        long taskListId = 2L;

        UpdateTaskListRequest updateRequest = new UpdateTaskListRequest();
        updateRequest.setName("Updated test task list");

        TaskListResponse expectedResult = new TaskListResponse(updateRequest.toTaskList());
        expectedResult.setId(taskListId);

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/task-lists/" + taskListId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskListResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldCompleteTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        when(taskListService.completeTaskList(taskListId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/task-lists/completed/" + taskListId)
                .exchange()

                .expectStatus().isNoContent();
        verify(taskListService, times(1)).completeTaskList(taskListId, user);
    }

    @Test
    void shouldDeleteTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        when(taskListService.deleteTaskList(taskListId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).delete()
                .uri("/api/v1/task-lists/" + taskListId).exchange()
                .expectStatus().isNoContent();
        verify(taskListService, times(1)).deleteTaskList(taskListId, user);
    }

    @Test
    void shouldReturnTasksForTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).userId(user.getId()).taskListId(3L).title("Test task").build();
        when(taskListService.getTasks(task.getTaskListId(), user, PageRequest.of(0, 20)))
                .thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/task-lists/" + task.getTaskListId() + "/tasks")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldAddTaskToTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        long taskId = 3L;
        when(taskListService.addTask(taskListId, taskId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/task-lists/" + taskListId + "/tasks/" + taskId)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldRemoveTaskFromTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        long taskId = 3L;
        when(taskListService.removeTask(taskListId, taskId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .delete().uri("/api/v1/task-lists/" + taskListId + "/tasks/" + taskId)
                .exchange()

                .expectStatus().isNoContent();
    }

    private Authentication createAuthentication(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
