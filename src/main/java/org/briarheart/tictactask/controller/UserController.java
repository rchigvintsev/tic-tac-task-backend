package org.briarheart.tictactask.controller;

import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.model.ProfilePicture;
import org.briarheart.tictactask.model.User;
import org.briarheart.tictactask.service.EmailConfirmationService;
import org.briarheart.tictactask.service.InvalidPasswordException;
import org.briarheart.tictactask.service.PasswordService;
import org.briarheart.tictactask.service.UserService;
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
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Locale;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/api/v1/users")
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
    public Mono<Long> getUserCount(Authentication authentication) {
        User user = getUser(authentication);
        if (!user.isAdmin()) {
            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        return userService.getUserCount();
    }

    @GetMapping
    public Flux<User> getUsers(Authentication authentication, Pageable pageable) {
        User user = getUser(authentication);
        if (!user.isAdmin()) {
            return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
        }
        return userService.getUsers(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody User user, Locale locale) {
        return userService.createUser(user, locale);
    }

    @PostMapping("/{id}/email/confirmation/{token}")
    public Mono<Void> confirmEmail(@PathVariable Long id, @PathVariable String token) {
        return emailConfirmationService.confirmEmail(id, token);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.OK)
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
    public Mono<Void> confirmPasswordReset(@PathVariable Long id,
                                           @PathVariable String token,
                                           ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            String newPassword = formData.getFirst("password");
            return passwordService.confirmPasswordReset(id, token, newPassword);
        });
    }

    @PostMapping("/{id}/password")
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
    public Mono<User> updateUser(@Valid @RequestBody User user, @PathVariable Long id, Authentication authentication) {
        user.setId(id);
        User currentUser = getUser(authentication);
        if (currentUser.getId().equals(id)) {
            user.setEnabled(null);
        }
        return ensureValidUserId(id, authentication).then(userService.updateUser(user));
    }

    @GetMapping(path = "/{id}/profile-picture")
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
}
