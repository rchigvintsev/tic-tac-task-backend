package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.model.TaskComment;
import org.briarheart.orchestra.model.User;
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

import static org.mockito.ArgumentMatchers.any;
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
        User user = User.builder().id(1L).build();
        Authentication authentication = createAuthentication(user);

        TaskComment comment = TaskComment.builder().commentText("Updated comment text").build();

        long commentId = 1L;

        TaskComment responseBody = comment.copy();
        responseBody.setId(commentId);
        responseBody.setUserId(user.getId());

        Mockito.when(taskCommentService.updateComment(any(TaskComment.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        testClient.mutateWith(mockAuthentication(authentication)).mutateWith(csrf())
                .put().uri("/task-comments/" + commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskComment.class).isEqualTo(responseBody);
    }

    @Test
    void shouldDeleteComment() {
        User user = User.builder().id(1L).build();
        Authentication authentication = createAuthentication(user);

        long commentId = 1L;
        Mockito.when(taskCommentService.deleteComment(commentId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authentication)).mutateWith(csrf())
                .delete().uri("/task-comments/" + commentId)
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
