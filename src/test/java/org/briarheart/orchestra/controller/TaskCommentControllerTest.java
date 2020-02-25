package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.service.TaskCommentService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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

import java.util.Locale;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TaskCommentController.class)
@Import(PermitAllSecurityConfig.class)
class TaskCommentControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TaskCommentService taskCommentService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void shouldGetCommentsForTask() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        TaskComment comment = TaskComment.builder().id(1L).taskId(2L).commentText("Test comment").build();
        Mockito.when(taskCommentService.getComments(comment.getTaskId(), authenticationMock.getName()))
                .thenReturn(Flux.just(comment));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/taskComments?taskId=" + comment.getTaskId()).exchange()
                .expectStatus().isOk()
                .expectBody(TaskComment[].class).isEqualTo(new TaskComment[]{comment});
    }

    @Test
    void shouldCreateComment() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        TaskComment comment = TaskComment.builder().commentText("New test comment").build();
        TaskComment savedComment = comment.copy();
        savedComment.setId(1L);
        savedComment.setTaskId(2L);
        Mockito.when(taskCommentService.createComment(comment, authenticationMock.getName(), savedComment.getTaskId()))
                .thenReturn(Mono.just(savedComment));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).post()
                .uri("/taskComments?taskId=" + savedComment.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/taskComments/" + savedComment.getId())
                .expectBody(TaskComment.class).isEqualTo(savedComment);
    }

    @Test
    void shouldUpdateComment() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        TaskComment comment = TaskComment.builder().id(1L).commentText("Test comment").build();
        TaskComment updatedComment = comment.copy();
        updatedComment.setId(1L);
        updatedComment.setCommentText("Updated test comment");
        Mockito.when(taskCommentService.updateComment(comment, comment.getId(), authenticationMock.getName()))
                .thenReturn(Mono.just(updatedComment));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).put()
                .uri("/taskComments/" + comment.getId() + "?taskId=" + updatedComment.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskComment.class).isEqualTo(updatedComment);
    }

    @Test
    void shouldDeleteComment() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        long commentId = 1L;
        Mockito.when(taskCommentService.deleteComment(commentId, authenticationMock.getName()))
                .thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).delete()
                .uri("/taskComments/" + commentId).exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldRejectCommentCreationWhenCommentTextIsNull() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        TaskComment comment = TaskComment.builder().taskId(1L).build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).post()
                .uri("/taskComments?taskId=" + comment.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.commentText").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectCommentCreationWhenCommentTextIsBlank() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        TaskComment comment = TaskComment.builder().taskId(1L).commentText("").build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).post()
                .uri("/taskComments?taskId=" + comment.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.commentText").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectCommentCreationWhenCommentTextIsTooLong() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        TaskComment comment = TaskComment.builder().taskId(1L).commentText("L" + "o".repeat(9993) + "ng text").build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).post()
                .uri("/taskComments?taskId=" + comment.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors.commentText").isEqualTo("Value length must not be greater than 10000");
    }
}
