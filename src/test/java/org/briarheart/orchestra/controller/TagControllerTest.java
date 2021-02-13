package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.TagService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TagController.class)
@Import(PermitAllSecurityConfig.class)
class TagControllerTest {
    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TagService tagService;

    @Test
    void shouldReturnAllTags() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Tag tag = Tag.builder().id(2L).name("Test tag").userId(user.getId()).build();
        when(tagService.getTags(user)).thenReturn(Flux.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags").exchange()
                .expectStatus().isOk()
                .expectBody(Tag[].class).isEqualTo(new Tag[] {tag});
    }

    @Test
    void shouldReturnTagById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Tag tag = Tag.builder().id(2L).name("Test tag").userId(user.getId()).build();
        when(tagService.getTag(tag.getId(), user)).thenReturn(Mono.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags/1").exchange()
                .expectStatus().isOk()
                .expectBody(Tag.class).isEqualTo(tag);
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTagIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Tag is not found";

        when(tagService.getTag(anyLong(), any(User.class)))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrors(List.of(errorMessage));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags/1").exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class).isEqualTo(errorResponse);
    }

    @Test
    void shouldReturnUncompletedTasksForTag() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long tagId = 2L;

        Task task = Task.builder().id(3L).title("Test task").userId(user.getId()).build();
        when(tagService.getUncompletedTasks(eq(tagId), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags/" + tagId + "/tasks/uncompleted").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldCreateTag() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long tagId = 2L;

        Tag tag = Tag.builder().name("New tag").build();
        Mockito.when(tagService.createTag(tag)).thenAnswer(args -> {
            Tag t = args.getArgument(0);
            t.setId(tagId);
            return Mono.just(t);
        });

        Tag responseBody = tag.copy();
        responseBody.setId(tagId);
        responseBody.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/tags")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(tag)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/tags/" + tagId)
                .expectBody(Tag.class).isEqualTo(responseBody);
    }

    @Test
    void shouldReturnBadRequestStatusCodeOnTagCreateWhenTagAlreadyExists() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Tag tag = Tag.builder().name("New tag").build();
        String errorMessage = "Tag already exists";
        when(tagService.createTag(tag))
                .thenReturn(Mono.error(new EntityAlreadyExistsException(errorMessage)));

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setErrors(List.of(errorMessage));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/tags")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(tag)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class).isEqualTo(errorResponse);
    }

    @Test
    void shouldUpdateTag() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        Mockito.when(tagService.updateTag(any(Tag.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        Tag tag = Tag.builder().name("Updated test tag").build();

        long tagId = 2L;

        Tag responseBody = tag.copy();
        tag.setId(tagId);
        tag.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).put()
                .uri("/tags/" + tagId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Tag.class).isEqualTo(responseBody);
    }

    @Test
    void shouldDeleteTag() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        long tagId = 2L;

        Mockito.when(tagService.deleteTag(tagId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).delete()
                .uri("/tags/" + tagId).exchange()
                .expectStatus().isNoContent();
    }

    private Authentication createAuthentication(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
