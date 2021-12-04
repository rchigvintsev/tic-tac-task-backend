package org.briarheart.tictactask.task.tag;

import org.briarheart.tictactask.config.PermitAllSecurityConfig;
import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.Task;
import org.briarheart.tictactask.task.TaskController.TaskResponse;
import org.briarheart.tictactask.task.tag.TagController.CreateTagRequest;
import org.briarheart.tictactask.task.tag.TagController.TagResponse;
import org.briarheart.tictactask.task.tag.TagController.UpdateTagRequest;
import org.briarheart.tictactask.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

import java.util.Locale;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(TagController.class)
@Import(PermitAllSecurityConfig.class)
class TagControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private TagService tagService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @Test
    void shouldReturnAllTags() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagService.getTags(user)).thenReturn(Flux.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tags")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TagResponse[].class).isEqualTo(new TagResponse[]{new TagResponse(tag)});
    }

    @Test
    void shouldReturnTagById() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Tag tag = Tag.builder().id(2L).userId(user.getId()).name("Test tag").build();
        when(tagService.getTag(tag.getId(), user)).thenReturn(Mono.just(tag));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tags/" + tag.getId())
                .exchange()

                .expectStatus().isOk()
                .expectBody(TagResponse.class).isEqualTo(new TagResponse(tag));
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenTagIsNotFoundById() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Tag is not found";
        when(tagService.getTag(anyLong(), eq(user))).thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tags/2")
                .exchange()

                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldReturnUncompletedTasksForTag() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Task task = Task.builder().id(2L).userId(user.getId()).title("Test task").build();

        long tagId = 3L;

        when(tagService.getUncompletedTasks(eq(tagId), eq(user), any())).thenReturn(Flux.just(task));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/tags/" + tagId + "/tasks/uncompleted")
                .exchange()

                .expectStatus().isOk()
                .expectBody(TaskResponse[].class).isEqualTo(new TaskResponse[]{new TaskResponse(task)});
    }

    @Test
    void shouldCreateTag() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long tagId = 2L;

        when(tagService.createTag(any(Tag.class))).thenAnswer(args -> {
            Tag t = args.getArgument(0);
            t.setId(tagId);
            return Mono.just(t);
        });

        CreateTagRequest createRequest = new CreateTagRequest();
        createRequest.setName("New tag");

        TagResponse expectedResult = new TagResponse(createRequest.toTag());
        expectedResult.setId(tagId);

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/v1/tags/" + tagId)
                .expectBody(TagResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectTagCreationWhenNameIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTagRequest createRequest = new CreateTagRequest();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTagCreationWhenNameIsBlank() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTagRequest createRequest = new CreateTagRequest();
        createRequest.setName(" ");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value must not be blank");
    }

    @Test
    void shouldRejectTagCreationWhenNameIsTooLong() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        CreateTagRequest createRequest = new CreateTagRequest();
        createRequest.setName("L" + "o".repeat(43) + "ng name");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Value length must not be greater than 50");
    }

    @Test
    void shouldReturnBadRequestStatusCodeOnTagCreateWhenTagAlreadyExists() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Tag already exists";
        when(tagService.createTag(any(Tag.class)))
                .thenReturn(Mono.error(new EntityAlreadyExistsException(errorMessage)));

        CreateTagRequest createRequest = new CreateTagRequest();
        createRequest.setName("New tag");

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldUpdateTag() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        when(tagService.updateTag(any(Tag.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        long tagId = 2L;

        UpdateTagRequest updateRequest = new UpdateTagRequest();
        updateRequest.setName("Updated test tag");

        TagResponse expectedResult = new TagResponse(updateRequest.toTag());
        expectedResult.setId(tagId);

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/tags/" + tagId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()

                .expectStatus().isOk()
                .expectBody(TagResponse.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldDeleteTag() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long tagId = 2L;

        when(tagService.deleteTag(tagId, user)).thenReturn(Mono.empty());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .delete().uri("/api/v1/tags/" + tagId)
                .exchange()

                .expectStatus().isNoContent();
        verify(tagService, times(1)).deleteTag(tagId, user);
    }

    private Authentication createAuthentication(User user) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(user.getEmail());
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
