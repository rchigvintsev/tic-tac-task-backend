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
                .uri("/task-comments/" + comment.getId())
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
                .uri("/task-comments/" + commentId).exchange()
                .expectStatus().isNoContent();
    }
}
