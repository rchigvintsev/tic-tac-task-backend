package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.TaskCommentRepository;
import org.briarheart.orchestra.model.TaskComment;
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

import java.time.LocalDateTime;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TaskCommentController.class)
@Import(PermitAllSecurityConfig.class)
class TaskCommentControllerTest {
    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TaskCommentRepository commentRepository;

    @Test
    void shouldGetCommentsForTask() {
        long taskId = 1L;
        TaskComment comment = TaskComment.builder()
                .id(1L)
                .taskId(taskId)
                .commentText("Test comment")
                .createdAt(LocalDateTime.now())
                .build();
        Flux<TaskComment> commentFlux = Flux.just(comment);
        Mockito.when(commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)).thenReturn(commentFlux);

        testClient.get().uri("/taskComments?taskId=" + taskId).exchange()
                .expectStatus().isOk()
                .expectBody(TaskComment[].class).isEqualTo(new TaskComment[] {comment});
    }

    @Test
    void shouldCreateComment() {
        long taskId = 1L;
        String commentText = "New comment";
        LocalDateTime createdAt = LocalDateTime.now();
        TaskComment comment = TaskComment.builder()
                .taskId(taskId)
                .commentText(commentText)
                .createdAt(createdAt)
                .build();
        TaskComment savedComment = TaskComment.builder()
                .id(2L)
                .taskId(taskId)
                .commentText(commentText)
                .createdAt(createdAt)
                .build();
        Mono<TaskComment> savedTaskMono = Mono.just(savedComment);
        Mockito.when(commentRepository.save(comment)).thenReturn(savedTaskMono);

        testClient.mutateWith(csrf()).post()
                .uri("/taskComments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(comment)
                .exchange()

                .expectStatus().isCreated()
                .expectBody(TaskComment.class).isEqualTo(savedComment);
    }

    @Test
    void shouldDeleteComment() {
        long commentId = 1L;
        Mockito.when(commentRepository.deleteById(commentId)).thenReturn(Mono.empty());
        testClient.mutateWith(csrf()).delete().uri("/taskComments/" + commentId).exchange()
                .expectStatus().isNoContent();
    }
}