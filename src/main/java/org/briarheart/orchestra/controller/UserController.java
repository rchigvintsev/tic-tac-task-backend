package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.User;
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
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> createUser(@Valid @RequestBody User user, Locale locale) {
        return userService.createUser(user, locale);
    }

    @PutMapping("/{id}/email/confirmation/{token}")
    public Mono<Void> confirmEmail(@PathVariable Long id, @PathVariable String token) {
        return userService.confirmEmail(id, token);
    }
}
