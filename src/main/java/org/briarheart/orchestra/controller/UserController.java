package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.EmailConfirmationService;
import org.briarheart.orchestra.service.PasswordService;
import org.briarheart.orchestra.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Locale;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/v1/users")
public class UserController extends AbstractController {
    private final UserService userService;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordService passwordService;

    /**
     * Creates new instance of this class with the given user, email confirmation, and password services.
     *
     * @param userService user service (must not be {@code null})
     * @param emailConfirmationService email confirmation service (must not be {@code null})
     * @param passwordService password service (must not be {@code null})
     */
    public UserController(UserService userService,
                          EmailConfirmationService emailConfirmationService,
                          PasswordService passwordService) {
        Assert.notNull(userService, "User service must not be null");
        Assert.notNull(emailConfirmationService, "Email confirmation service must not be null");
        Assert.notNull(passwordService, "Password service must not be null");

        this.userService = userService;
        this.emailConfirmationService = emailConfirmationService;
        this.passwordService = passwordService;
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
            return userService.resetPassword(email, locale);
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

    @PutMapping("/{id}")
    public Mono<User> updateUser(@Valid @RequestBody User user, @PathVariable Long id, Authentication authentication) {
        user.setId(id);
        user.setEmail(getUser(authentication).getEmail());
        return userService.updateUser(user);
    }
}
