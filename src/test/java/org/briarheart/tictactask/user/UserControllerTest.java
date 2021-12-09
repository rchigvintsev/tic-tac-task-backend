package org.briarheart.tictactask.user;

import org.briarheart.tictactask.config.I18nConfig;
import org.briarheart.tictactask.config.PermitAllSecurityConfig;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.user.UserController.CreateUserRequest;
import org.briarheart.tictactask.user.UserController.UpdateUserRequest;
import org.briarheart.tictactask.user.UserController.UserResponse;
import org.briarheart.tictactask.user.email.EmailConfirmationService;
import org.briarheart.tictactask.user.password.InvalidPasswordException;
import org.briarheart.tictactask.user.password.PasswordService;
import org.briarheart.tictactask.user.profilepicture.ProfilePicture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
@WebFluxTest(UserController.class)
@Import({PermitAllSecurityConfig.class, I18nConfig.class})
@ActiveProfiles("test")
class UserControllerTest {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    @Autowired
    private WebTestClient testClient;

    @MockBean
    private UserService userService;
    @MockBean
    private EmailConfirmationService emailConfirmationService;
    @MockBean
    private PasswordService passwordService;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterAll
    static void afterAll() {
        Locale.setDefault(DEFAULT_LOCALE);
    }

    @BeforeEach
    void setUp() {
        when(userService.updateUser(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));
    }

    @Test
    void shouldThrowExceptionOnConstructWhenMessageSourceAccessorIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new UserController(userService, emailConfirmationService, passwordService, null));
        assertEquals("Message source accessor must not be null", e.getMessage());
    }

    @Test
    void shouldReturnNumberOfAllUsers() {
        User user = User.builder().id(1L).email("alice@mail.com").admin(true).build();
        Authentication authenticationMock = createAuthentication(user);

        long userCount = 3L;
        when(userService.getUserCount()).thenReturn(Mono.just(userCount));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/users/count")
                .exchange()

                .expectStatus().isOk()
                .expectBody(Long.class).isEqualTo(userCount);
    }

    @Test
    void shouldReturnForbiddenStatusCodeOnUserCountGetWhenCurrentUserIsNotAdmin() {
        User user = User.builder().id(1L).email("alice@mail.com").admin(false).build();
        Authentication authenticationMock = createAuthentication(user);
        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/users/count")
                .exchange()

                .expectStatus().isForbidden();
    }

    @Test
    void shouldReturnAllUsers() {
        User user = User.builder().id(1L).email("alice@mail.com").admin(true).authorities(null).build();
        Authentication authenticationMock = createAuthentication(user);

        when(userService.getUsers(any())).thenReturn(Flux.just(user));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/users")
                .exchange()

                .expectStatus().isOk()
                .expectBody(UserResponse[].class).isEqualTo(new UserResponse[]{new UserResponse(user)});
    }

    @Test
    void shouldReturnForbiddenStatusCodeOnUsersGetWhenCurrentUserIsNotAdmin() {
        User user = User.builder().id(1L).email("alice@mail.com").admin(false).build();
        Authentication authenticationMock = createAuthentication(user);
        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/users")
                .exchange()

                .expectStatus().isForbidden();
    }

    @Test
    void shouldCreateUser() {
        when(userService.createUser(any(User.class), eq(Locale.ENGLISH)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setEmail("alice@mail.com");
        createRequest.setPassword("secret");
        createRequest.setFullName("Alice");

        testClient.mutateWith(csrf())
                .post().uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Accept-Language", "en")
                .bodyValue(createRequest)
                .exchange()

                .expectStatus().isCreated();
        verify(userService, times(1)).createUser(any(User.class), eq(Locale.ENGLISH));
    }

    @Test
    void shouldConfirmEmail() {
        long userId = 1L;
        String token = "K1Mb2ByFcfYndPmuFijB";
        when(emailConfirmationService.confirmEmail(userId, token)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/api/v1/users/{userId}/email/confirmation/{token}", userId, token)
                .exchange()

                .expectStatus().isOk();
        verify(emailConfirmationService, times(1)).confirmEmail(userId, token);
    }

    @Test
    void shouldResetPassword() {
        String email = "alice@mail.com";
        when(passwordService.resetPassword(email, Locale.ENGLISH)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/api/v1/users/password/reset")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("email=" + email)
                .header("Accept-Language", "en")
                .exchange()

                .expectStatus().isOk();
        verify(passwordService, times(1)).resetPassword(email, Locale.ENGLISH);
    }

    @Test
    void shouldReturnBadRequestStatusCodeOnPasswordResetWhenEmailFormParameterIsNotProvided() {
        testClient.mutateWith(csrf())
                .post().uri("/api/v1/users/password/reset")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Accept-Language", "en")
                .exchange()

                .expectStatus().isBadRequest();
    }

    @Test
    void shouldConfirmPasswordReset() {
        long userId = 1L;
        String token = "K1Mb2ByFcfYndPmuFijB";
        String newPassword = "qwerty";
        when(passwordService.confirmPasswordReset(userId, token, newPassword)).thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf())
                .post().uri("/api/v1/users/{userId}/password/reset/confirmation/{token}", userId, token)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("password=" + newPassword)
                .exchange()

                .expectStatus().isOk();
        verify(passwordService, times(1)).confirmPasswordReset(userId, token, newPassword);
    }

    @Test
    void shouldChangePassword() {
        User authenticatedUser = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        Authentication authenticationMock = createAuthentication(authenticatedUser);

        long userId = 1L;
        String newPassword = "s3cret";
        when(passwordService.changePassword(userId, authenticatedUser.getPassword(), newPassword))
                .thenReturn(Mono.just(true).then());

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .post().uri("/api/v1/users/{id}/password", userId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("currentPassword=" + authenticatedUser.getPassword() + "&newPassword=" + newPassword)
                .exchange()

                .expectStatus().isOk();
        verify(passwordService, times(1)).changePassword(userId, authenticatedUser.getPassword(), newPassword);
    }

    @Test
    void shouldRejectPasswordChangingWhenCurrentPasswordIsNotValid() {
        User authenticatedUser = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        Authentication authenticationMock = createAuthentication(authenticatedUser);

        long userId = 1L;
        String currentPassword = "qwerty";
        String newPassword = "s3cret";
        when(passwordService.changePassword(userId, currentPassword, newPassword))
                .thenReturn(Mono.error(new InvalidPasswordException(currentPassword)));

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .post().uri("/api/v1/users/{id}/password", userId)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("currentPassword=" + currentPassword + "&newPassword=" + newPassword)
                .exchange()

                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.fieldErrors").exists()
                .jsonPath("$.fieldErrors[0].message").isEqualTo("Invalid password");
    }

    @Test
    void shouldUpdateUser() {
        User authenticatedUser = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .build();
        Authentication authenticationMock = createAuthentication(authenticatedUser);

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFullName("Alice Wonderland");

        UserResponse expectedResult = new UserResponse(authenticatedUser);
        expectedResult.setFullName(updateRequest.getFullName());

        testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .put().uri("/api/v1/users/{userId}", authenticatedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()

                .expectStatus().isOk();

        verify(userService, times(1)).updateUser(any(User.class));
    }

    @Test
    void shouldNotAllowToChangeUsersOwnEnabledFieldOnUserUpdate() {
        User authenticatedUser = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .admin(true)
                .build();
        Authentication authenticationMock = createAuthentication(authenticatedUser);

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEnabled(false);

        UserResponse response = testClient.mutateWith(csrf()).mutateWith(mockAuthentication(authenticationMock))
                .put().uri("/api/v1/users/{userId}", authenticatedUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()

                .expectStatus().isOk()

                .returnResult(UserResponse.class).getResponseBody().blockFirst();

        assertNotNull(response);
        assertTrue(response.getEnabled());
    }

    @Test
    void shouldReturnProfilePicture() throws IOException {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        byte[] imageData = new ClassPathResource("test-image.png").getInputStream().readAllBytes();
        ProfilePicture profilePicture = ProfilePicture.builder()
                .userId(user.getId())
                .data(imageData)
                .type(MediaType.IMAGE_PNG_VALUE)
                .build();
        when(userService.getProfilePicture(user.getId())).thenReturn(Mono.just(profilePicture));

        testClient.mutateWith(mockAuthentication(authenticationMock))
                .get().uri("/api/v1/users/{userId}/profile-picture", user.getId())
                .exchange()

                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectBody(byte[].class).isEqualTo(profilePicture.getData());
    }

    @Test
    void shouldReturnNotFoundStatusCodeWhenProfilePictureIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        String errorMessage = "Image is not found";
        when(userService.getProfilePicture(user.getId()))
                .thenReturn(Mono.error(new EntityNotFoundException(errorMessage)));

        testClient.mutateWith(mockAuthentication(authenticationMock)).get()
                .uri("/api/v1/users/{userId}/profile-picture", user.getId()).exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo(errorMessage);
    }

    @Test
    void shouldSaveProfilePicture() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(userService.saveProfilePicture(any(ProfilePicture.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("profilePicture", new ClassPathResource("test-image.png"))
                .filename("test-image.png")
                .contentType(MediaType.IMAGE_PNG);
        MultiValueMap<String, HttpEntity<?>> form = multipartBodyBuilder.build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/users/{userId}/profile-picture", user.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .exchange()

                .expectStatus().isNoContent();
    }

    @Test
    void shouldUpdateUserProfilePictureUrlOnProfilePictureSave() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Authentication authenticationMock = createAuthentication(user);

        when(userService.saveProfilePicture(any(ProfilePicture.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("profilePicture", new ClassPathResource("test-image.png"))
                .filename("test-image.png")
                .contentType(MediaType.IMAGE_PNG);
        MultiValueMap<String, HttpEntity<?>> form = multipartBodyBuilder.build();

        testClient.mutateWith(mockAuthentication(authenticationMock)).mutateWith(csrf())
                .put().uri("/api/v1/users/{userId}/profile-picture", user.getId())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(form)
                .exchange();

        assertEquals("/api/v1/users/" + user.getId() + "/profile-picture", user.getProfilePictureUrl());
    }

    private Authentication createAuthentication(User user) {
        Authentication authenticationMock = mock(Authentication.class);
        when(authenticationMock.getName()).thenReturn(user.getEmail());
        when(authenticationMock.getPrincipal()).thenReturn(user);
        return authenticationMock;
    }
}
