package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.TaskRepository;
import org.briarheart.orchestra.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

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
        Task task = Task.builder().id(1L).title("Test task").build();
        Flux<Task> taskFlux = Flux.just(task);
        Mockito.when(taskRepository.findByCompleted(false)).thenReturn(taskFlux);

        testClient.get().uri("/tasks?completed=false").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldGetAllCompletedTasks() {
        Task task = Task.builder().id(1L).title("Test task").completed(true).build();
        Flux<Task> taskFlux = Flux.just(task);
        Mockito.when(taskRepository.findByCompleted(true)).thenReturn(taskFlux);

        testClient.get().uri("/tasks?completed=true").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldGetTaskById() {
        Task task = Task.builder().id(1L).title("Test task").build();
        Mono<Task> taskMono = Mono.just(task);
        Mockito.when(taskRepository.findById(1L)).thenReturn(taskMono);

        testClient.get().uri("/tasks/1").exchange()
                .expectStatus().isOk()
                .expectBody(Task.class).isEqualTo(task);
    }

    @Test
    void shouldCreateTask() {
        String taskTitle = "New task";
        Task task = Task.builder().title(taskTitle).build();
        Task savedTask = Task.builder().id(2L).title(taskTitle).build();
        Mono<Task> savedTaskMono = Mono.just(savedTask);
        Mockito.when(taskRepository.save(task)).thenReturn(savedTaskMono);

        testClient.mutateWith(csrf()).post()
                .uri("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(task)
                .exchange()

                .expectStatus().isCreated()
                .expectBody(Task.class).isEqualTo(savedTask);
    }
}