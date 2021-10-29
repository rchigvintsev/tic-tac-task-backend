package org.briarheart.tictactask.task.tag;

import org.briarheart.tictactask.config.PermitAllSecurityConfig;
import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.task.Task;
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
                .expectBody(Tag[].class).isEqualTo(new Tag[]{tag});
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
                .expectBody(Tag.class).isEqualTo(tag);
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
                .expectBody(Task[].class).isEqualTo(new Task[]{task});
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

        Tag tag = Tag.builder().name("New tag").build();

        Tag expectedResult = new Tag(tag);
        expectedResult.setId(tagId);
        expectedResult.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
                .exchange()

                .expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "/api/v1/tags/" + tagId)
                .expectBody(Tag.class).isEqualTo(expectedResult);
    }

    @Test
    void shouldRejectTagCreationWhenNameIsNull() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        Authentication authenticationMock = createAuthentication(user);

        Tag tag = Tag.builder().build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
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

        Tag tag = Tag.builder().name(" ").build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
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

        Tag tag = Tag.builder().name("L" + "o".repeat(43) + "ng name").build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
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

        Tag tag = Tag.builder().name("New tag").build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .post().uri("/api/v1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
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

        Tag tag = Tag.builder().name("Updated test tag").build();

        Tag expectedResult = new Tag(tag);
        expectedResult.setId(tagId);
        expectedResult.setUserId(user.getId());

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/tags/" + tagId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tag)
                .exchange()

                .expectStatus().isOk()
                .expectBody(Tag.class).isEqualTo(expectedResult);
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
