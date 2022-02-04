package org.briarheart.tictactask.task.comment;

import org.briarheart.tictactask.config.PermitAllSecurityConfig;
import org.briarheart.tictactask.task.comment.TaskCommentController.TaskCommentResponse;
import org.briarheart.tictactask.task.comment.TaskCommentController.UpdateTaskCommentRequest;
import org.briarheart.tictactask.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@WebFluxTest(TaskCommentController.class)
@Import(PermitAllSecurityConfig.class)
@ActiveProfiles("test")
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
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authentication = createAuthentication(user);

        when(taskCommentService.updateComment(any(TaskComment.class)))
                .thenAnswer(args -> Mono.just(new TaskComment(args.getArgument(0))));

        long commentId = 2L;

        UpdateTaskCommentRequest updateRequest = new UpdateTaskCommentRequest();
        updateRequest.setCommentText("Updated test comment text");

        TaskCommentResponse expectedResult = new TaskCommentResponse(updateRequest.toTaskComment());
        expectedResult.setId(commentId);

        testClient.mutateWith(mockAuthentication(authentication)).mutateWith(csrf())
                .put().uri("/api/v1/task-comments/" + commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskCommentResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldDeleteComment() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authentication = createAuthentication(user);

        long commentId = 2L;

        when(taskCommentService.deleteComment(commentId, user)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(mockAuthentication(authentication)).mutateWith(csrf())
                .delete().uri("/api/v1/task-comments/" + commentId)
                .exchange()

                .expectStatus().isNoContent();
        verify(taskCommentService, times(1)).deleteComment(commentId, user);
    }

    private Authentication createAuthentication(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
