package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
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

import static org.mockito.ArgumentMatchers.*;
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
    private TaskRepository taskRepository;

    @Test
    void shouldGetAllUncompletedTasks() {
        String username = "alice";

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Task task = Task.builder().id(1L).title("Test task").author(username).build();
        when(taskRepository.findByCompletedAndAuthor(false, username)).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks?completed=false").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldGetAllCompletedTasks() {
        String username = "alice";

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Task task = Task.builder().id(1L).title("Test task").author(username).completed(true).build();
        when(taskRepository.findByCompletedAndAuthor(true, username)).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks?completed=true").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldGetTaskById() {
        String username = "alice";

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Task task = Task.builder().id(1L).title("Test task").author(username).build();
        when(taskRepository.findByIdAndAuthor(1L, username)).thenReturn(Mono.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/1").exchange()
                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(task);
    }

    @Test
    void shouldCreateTask() {
        String taskTitle = "New task";
        String taskAuthor = "alice";

        Task task = Task.builder().title(taskTitle).author(taskAuthor).build();
        Task savedTask = Task.builder().id(2L).title(taskTitle).author(taskAuthor).build();

        when(taskRepository.save(task)).thenReturn(Mono.just(savedTask));

        testClient.mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isCreated()
                .expectBody(Task.class).isEqualTo(savedTask);
    }

    @Test
    void shouldNotGetTasksBelongingToOtherUsers() {
        String username = "alice";

        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Task expectedTask = Task.builder().id(1L).title("Expected task").author(username).build();
        Task unexpectedTask = Task.builder().id(1L).title("Unexpected task").author("bob").build();

        when(taskRepository.findByCompletedAndAuthor(anyBoolean(), any())).thenAnswer(invocation -> {
            String author = invocation.getArgument(1, String.class);
            if (expectedTask.getAuthor().equals(author)) {
                return Flux.just(expectedTask);
            }
            if (unexpectedTask.getAuthor().equals(author)) {
                return Flux.just(unexpectedTask);
            }
            return Flux.empty();
        });

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {expectedTask});
    }

    @Test
    void shouldNotGetTaskByIdBelongingToOtherUser() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Task task = Task.builder().id(1L).title("Test task").author("bob").build();
        when(taskRepository.findByIdAndAuthor(anyLong(), any())).thenAnswer(invocation -> {
            String author = invocation.getArgument(1, String.class);
            if (task.getAuthor().equals(author)) {
                return Mono.just(task);
            }
            return Mono.empty();
        });

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage("Task with id " + task.getId() + " is not found");
        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tasks/1").exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .isEqualTo(errorResponse);
    }
}
