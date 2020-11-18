package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.config.PermitAllSecurityConfig;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Tag;
import org.briarheart.orchestra.model.Task;
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
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Tag tag = Tag.builder().id(1L).name("Test tag").author(authenticationMock.getName()).build();
        when(tagService.getTags(eq(authenticationMock.getName()))).thenReturn(Flux.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags").exchange()
                .expectStatus().isOk()
                .expectBody(Tag[].class).isEqualTo(new Tag[] {tag});
    }

    @Test
    void shouldReturnTagById() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        Tag tag = Tag.builder().id(1L).name("Test tag").author(authenticationMock.getName()).build();
        when(tagService.getTag(tag.getId(), authenticationMock.getName())).thenReturn(Mono.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags/1").exchange()
                .expectStatus().isOk()
                .expectBody(Tag.class).isEqualTo(tag);
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTagIsNotFoundById() {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn("alice");

        String errorMessage = "Tag is not found";

        when(tagService.getTag(anyLong(), anyString()))
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
        String username = "alice";
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        long tagId = 1L;

        Task task = Task.builder().id(2L).title("Test task").author(username).build();
        when(tagService.getUncompletedTasks(eq(tagId), eq(username), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/tags/" + tagId + "/tasks/uncompleted").exchange()
                .expectStatus().isOk()
                .expectBody(Task[].class).isEqualTo(new Task[] {task});
    }

    @Test
    void shouldCreateTag() {
        String username = "alice";
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Tag tag = Tag.builder().name("New tag").build();
        Tag savedTag = tag.copy();
        savedTag.setId(1L);
        savedTag.setAuthor(username);

        Mockito.when(tagService.createTag(tag, username)).thenReturn(Mono.just(savedTag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/tags")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(tag)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/tags/" + savedTag.getId())
                .expectBody(Tag.class).isEqualTo(savedTag);
    }

    @Test
    void shouldReturnBadRequestStatusCodeOnTagCreateWhenTagAlreadyExists() {
        String username = "alice";
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Tag tag = Tag.builder().name("New tag").build();
        String errorMessage = "Tag already exists";
        when(tagService.createTag(tag, username))
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
        String username = "alice";
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        Tag tag = Tag.builder().id(1L).name("Test tag").build();
        Tag updatedTag = tag.copy();
        updatedTag.setId(1L);
        updatedTag.setName("Updated test tag");
        Mockito.when(tagService.updateTag(tag, tag.getId(), username)).thenReturn(Mono.just(updatedTag));

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).put()
                .uri("/tags/" + tag.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Tag.class).isEqualTo(updatedTag);
    }

    @Test
    void shouldDeleteTag() {
        String username = "alice";
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(username);

        long tagId = 1L;
        Mockito.when(tagService.deleteTag(tagId, username)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf()).delete()
                .uri("/tags/" + tagId).exchange()
                .expectStatus().isNoContent();
    }
}
