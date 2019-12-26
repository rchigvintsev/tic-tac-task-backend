package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TaskController.class)
@Import(PermitAllSecurityConfig.class)
class TaskControllerTest {
    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TaskService taskService;

    @Test
    void shouldReturnAllUncompletedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author(authenticationMock.getName()).build();
        when(taskService.getUncompletedTasks(authenticationMock.getName())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks?completed=false").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnAllCompletedTasks() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder()
                .id(1L)
                .title("Test task")
                .author(authenticationMock.getName())
                .completed(true)
                .build();
        when(taskService.getCompletedTasks(authenticationMock.getName())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks?completed=true").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldReturnTaskById() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author(authenticationMock.getName()).build();
        when(taskService.getTask(task.getId(), authenticationMock.getName())).thenReturn(Mono.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/1").exchange()
                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(task);
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTaskIsNotFoundById() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        String errorMessage = "Task is not found";

        when(taskService.getTask(anyLong(), anyString()))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(errorMessage);

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/1").exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class).isEqualTo(errorResponse);
    }

    @Test
    void shouldCreateTask() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().title("New task").build();
        Task savedTask = Task.builder().id(2L).title(task.getTitle()).author(authenticationMock.getName()).build();

        when(taskService.createTask(task, authenticationMock.getName())).thenReturn(Mono.just(savedTask));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isCreated()
                .expectBody(Task.class).isEqualTo(savedTask);
    }

    @Test
    void shouldUpdateTask() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(2L).title("Test task").author(authenticationMock.getName()).build();
        Task updatedTask = Task.builder().title("Updated test task").build();
        when(taskService.getTask(task.getId(), authenticationMock.getName())).thenReturn(Mono.just(task));
        when(taskService.updateTask(updatedTask, task.getId(), authenticationMock.getName()))
                .thenReturn(Mono.just(updatedTask));

        testClient.mutateWith(csrf())
                .mutateWith(mockAuthentication(authenticationMock))
                .put()
                .uri("/tasks/2")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedTask)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(updatedTask);
    }
}
