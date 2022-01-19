package org.briarheart.tictactask.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import org.briarheart.tictactask.controller.AbstractController;
import org.briarheart.tictactask.controller.RequiredFormParameterMissingException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.user.email.EmailConfirmationService;
import org.briarheart.tictactask.user.password.InvalidPasswordException;
import org.briarheart.tictactask.user.password.PasswordService;
import org.briarheart.tictactask.user.profilepicture.ProfilePicture;
import org.briarheart.tictactask.util.Errors;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "Users",
        description = "Allows to manage users as well as change user profile picture and password"
)
public class UserController extends AbstractController {
    private final UserService userService;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordService passwordService;
    private final MessageSourceAccessor messages;

    /**
     * Creates new instance of this class with the given user, email confirmation, and password services.
     *
     * @param userService              user service (must not be {@code null})
     * @param emailConfirmationService email confirmation service (must not be {@code null})
     * @param passwordService          password service (must not be {@code null})
     * @param messages                 source of localized messages (must not be {@code null})
     */
    public UserController(UserService userService,
                          EmailConfirmationService emailConfirmationService,
                          PasswordService passwordService,
                          MessageSourceAccessor messages) {
        Assert.notNull(userService, "User service must not be null");
        Assert.notNull(emailConfirmationService, "Email confirmation service must not be null");
        Assert.notNull(passwordService, "Password service must not be null");
        Assert.notNull(messages, "Message source accessor must not be null");

        this.userService = userService;
        this.emailConfirmationService = emailConfirmationService;
        this.passwordService = passwordService;
        this.messages = messages;
    }

    @GetMapping("/count")
    @Operation(summary = "Get total number of users", description = "Returns total number of registered users")
    public Mono<Long> getUserCount(Authentication authentication) {
        User user = getUser(authentication);
        if (!user.isAdmin()) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        return userService.getUserCount();
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Returns all registered users")
    public Flux<UserResponse> getUsers(Authentication authentication, Pageable pageable) {
        User user = getUser(authentication);
        if (!user.isAdmin()) {
            return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        return userService.getUsers(pageable).map(UserResponse::new);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new user", description = "Allows to register new user")
    public Mono<UserResponse> createUser(@Valid @RequestBody CreateUserRequest createRequest, Locale locale) {
        return userService.createUser(createRequest.toUser(), locale).map(UserResponse::new);
    }

    @PostMapping("/{id}/email/confirmation/{token}")
    @Operation(summary = "Confirm user email", description = "Completes user registration by confirming user email")
    public Mono<Void> confirmEmail(@PathVariable Long id, @PathVariable String token) {
        return emailConfirmationService.confirmEmail(id, token);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Reset user password", description = "Sends password reset link to user email")
    public Mono<Void> resetPassword(ServerWebExchange exchange, Locale locale) {
        return exchange.getFormData().flatMap(formData -> {
            String email = formData.getFirst("email");
            if (!StringUtils.hasLength(email)) {
                String errorMessage = "A value for \"email\" form parameter must be provided";
                return Mono.error(new RequiredFormParameterMissingException(errorMessage));
            }
            return passwordService.resetPassword(email, locale);
        });
    }

    @PostMapping("/{id}/password/reset/confirmation/{token}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Confirm password reset",
            description = "Allows to set new value for user password if valid password reset token is provided"
    )
    public Mono<Void> confirmPasswordReset(@PathVariable Long id,
                                           @PathVariable String token,
                                           ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            String newPassword = formData.getFirst("password");
            return passwordService.confirmPasswordReset(id, token, newPassword);
        });
    }

    @PostMapping("/{id}/password")
    @Operation(summary = "Change user password", description = "Allows to change user password")
    public Mono<Void> changePassword(@PathVariable Long id,
                                     ServerWebExchange exchange,
                                     Authentication authentication) {
        return ensureValidUserId(id, authentication).then(exchange.getFormData()
                .flatMap(formData -> {
                    String currentPassword = formData.getFirst("currentPassword");
                    String newPassword = formData.getFirst("newPassword");
                    return passwordService.changePassword(id, currentPassword, newPassword);
                })
                .onErrorMap(InvalidPasswordException.class, e -> {
                    String message = messages.getMessage("invalid-password");
                    return Errors.createFieldError("currentPassword", e.getPassword(), message);
                }));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Allows to change some user attributes")
    public Mono<UserResponse> updateUser(@Valid @RequestBody UpdateUserRequest updateRequest,
                                         @PathVariable Long id,
                                         Authentication authentication) {
        User user = updateRequest.toUser();
        user.setId(id);
        User currentUser = getUser(authentication);
        if (currentUser.getId().equals(id)) {
            user.setEnabled(currentUser.isEnabled());
        }
        return ensureValidUserId(id, authentication).then(userService.updateUser(user)).map(UserResponse::new);
    }

    @GetMapping(path = "/{id}/profile-picture")
    @Operation(summary = "Get user profile picture", description = "Returns user profile picture")
    public Mono<ResponseEntity<Resource>> getProfilePicture(
            @PathVariable("id") Long id,
            Authentication authentication
    ) {
        return ensureValidUserId(id, authentication).then(userService.getProfilePicture(id).map(picture -> {
            ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
            if (picture.getType() != null) {
                bodyBuilder.contentType(MediaType.parseMediaType(picture.getType()));
            }
            return bodyBuilder.contentLength(picture.getData().length).body(new ByteArrayResource(picture.getData()));
        }));
    }

    @PutMapping("/{id}/profile-picture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Save user profile picture", description = "Allows to set custom user profile picture")
    public Mono<Void> saveProfilePicture(
            @PathVariable Long id,
            @RequestPart("profilePicture") Mono<FilePart> profilePicture,
            Authentication authentication,
            ServerHttpRequest request
    ) {
        User currentUser = getUser(authentication);
        return ensureValidUserId(id, currentUser).then(profilePicture.flatMapMany(Part::content)
                .reduce(new ByteArrayOutputStream(), (buffer, content) -> {
                    buffer.writeBytes(content.asByteBuffer().array());
                    DataBufferUtils.release(content);
                    return buffer;
                }).zipWith(profilePicture.map(Part::headers))
                .flatMap(contentAndHeaders -> {
                    byte[] pictureBytes = contentAndHeaders.getT1().toByteArray();
                    HttpHeaders headers = contentAndHeaders.getT2();
                    MediaType contentType = headers.getContentType();
                    String pictureType = contentType != null ? contentType.toString() : null;
                    ProfilePicture picture = ProfilePicture.builder()
                            .userId(id)
                            .data(pictureBytes)
                            .type(pictureType)
                            .build();
                    return userService.saveProfilePicture(picture);
                }).flatMap(picture -> {
                    URI profilePictureUri = UriComponentsBuilder.fromHttpRequest(request).build().toUri();
                    currentUser.setProfilePictureUrl(profilePictureUri.toString());
                    return userService.updateUser(currentUser);
                }).then());
    }

    private Mono<Void> ensureValidUserId(Long id, Authentication authentication) {
        return ensureValidUserId(id, getUser(authentication));
    }

    private Mono<Void> ensureValidUserId(Long id, User user) {
        if (!user.isAdmin() && !user.getId().equals(id)) {
            return Mono.error(new EntityNotFoundException("User with id " + id + " is not found"));
        }
        return Mono.empty();
    }

    @Data
    @NoArgsConstructor
    public static class UserResponse {
        private Long id;
        private String email;
        private String password;
        private boolean admin;
        private Boolean enabled;
        private String fullName;
        private String profilePictureUrl;
        private LocalDateTime createdAt;

        public UserResponse(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.password = user.getPassword();
            this.admin = user.isAdmin();
            this.enabled = user.isEnabled();
            this.fullName = user.getFullName();
            this.profilePictureUrl = user.getProfilePictureUrl();
            this.createdAt = user.getCreatedAt();
        }
    }

    @Data
    public static abstract class CreateOrUpdateUserRequest {
        @Size(max = 255)
        private String fullName;

        public User toUser() {
            return User.builder().fullName(fullName).build();
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class CreateUserRequest extends CreateOrUpdateUserRequest {
        @NotBlank
        @Size(max = 255)
        private String email;
        @Size(max = 50)
        private String password;

        @Override
        public User toUser() {
            User user = super.toUser();
            user.setEmail(email);
            user.setPassword(password);
            return user;
        }
    }

    @Getter
    @Setter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class UpdateUserRequest extends CreateOrUpdateUserRequest {
        private Boolean enabled;

        @Override
        public User toUser() {
            User user = super.toUser();
            if (enabled != null) {
                user.setEnabled(enabled);
            }
            return user;
        }
    }
}
