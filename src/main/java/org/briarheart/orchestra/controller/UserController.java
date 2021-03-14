package org.briarheart.orchestra.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.EmailConfirmationService;
import org.briarheart.orchestra.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Locale;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    @NonNull
    private final UserService userService;
    @NonNull
    private final EmailConfirmationService emailConfirmationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody User user, Locale locale) {
        return userService.createUser(user, locale);
    }

    @PutMapping("/{id}/email/confirmation/{token}")
    public Mono<Void> confirmEmail(@PathVariable Long id, @PathVariable String token) {
        return emailConfirmationService.confirmEmail(id, token);
    }
}
