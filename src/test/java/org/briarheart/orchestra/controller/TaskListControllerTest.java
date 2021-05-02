package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskList;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.TaskListService;
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
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        when(taskListService.getUncompletedTaskLists(user)).thenReturn(Flux.just(taskList));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/v1/task-lists/uncompleted")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskList[].class).isEqualTo(new TaskList[] {taskList});
    }

    @Test
    void shouldReturnTaskListById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskList taskList = TaskList.builder().id(2L).userId(user.getId()).name("Test task list").build();
        when(taskListService.getTaskList(taskList.getId(), user)).thenReturn(Mono.just(taskList));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/v1//task-lists/" + taskList.getId())
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskList.class).isEqualTo(taskList);
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTaskListIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Task list is not found";
        when(taskListService.getTaskList(anyLong(), any(User.class)))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/v1/task-lists/2")
                .exchange()

                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldCreateTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        when(taskListService.createTaskList(any(TaskList.class))).thenAnswer(args -> {
            TaskList l = args.getArgument(0);
            l.setId(taskListId);
            return Mono.just(l);
        });

        TaskList newTaskList = TaskList.builder().name("New task list").build();

        TaskList expectedResult = new TaskList(newTaskList);
        expectedResult.setId(taskListId);
        expectedResult.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/v1/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newTaskList)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/v1/task-lists/" + taskListId)
                .expectBody(TaskList.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectTaskListCreationWhenNameIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskList newTaskList = TaskList.builder().build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/v1/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newTaskList)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTaskListCreationWhenNameIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        TaskList taskList = TaskList.builder().name(" ").build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/v1/task-lists")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(taskList)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldUpdateTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(taskListService.updateTaskList(any(TaskList.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        long taskListId = 2L;

        TaskList taskList = TaskList.builder().name("Updated test task list").build();

        TaskList expectedResult = new TaskList(taskList);
        expectedResult.setId(taskListId);
        expectedResult.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/v1/task-lists/" + taskListId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(taskList)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskList.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldCompleteTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        when(taskListService.completeTaskList(taskListId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/v1/task-lists/completed/" + taskListId)
                .exchange()

                .expectStatus().isNoContent();
        verify(taskListService, times(1)).completeTaskList(taskListId, user);
    }

    @Test
    void shouldDeleteTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        when(taskListService.deleteTaskList(taskListId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).delete()
                .uri("/v1/task-lists/" + taskListId).exchange()
                .expectStatus().isNoContent();
        verify(taskListService, times(1)).deleteTaskList(taskListId, user);
    }

    @Test
    void shouldReturnTasksForTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).userId(user.getId()).taskListId(3L).title("Test task").build();
        when(taskListService.getTasks(task.getTaskListId(), user, PageRequest.of(0, 20)))
                .thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/v1/task-lists/" + task.getTaskListId() + "/tasks")
                .exchange()

                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
    }

    @Test
    void shouldAddTaskToTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        long taskId = 3L;
        when(taskListService.addTask(taskListId, taskId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/v1/task-lists/" + taskListId + "/tasks/" + taskId)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldRemoveTaskFromTaskList() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long taskListId = 2L;
        long taskId = 3L;
        when(taskListService.removeTask(taskListId, taskId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .delete().uri("/v1/task-lists/" + taskListId + "/tasks/" + taskId)
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
