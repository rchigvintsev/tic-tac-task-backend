package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link UserService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<User> createUser(User user) {
        Assert.notNull(user, "User must not be null");
        return userRepository.findById(user.getEmail())
                .flatMap(u -> {
                    String errorMessage = "User with email \"" + user.getEmail() + "\" is already registered";
                    return Mono.<User>error(new EntityAlreadyExistsException(errorMessage));
                })
                .switchIfEmpty(Mono.fromCallable(() -> User.builder()
                        .email(user.getEmail())
                        .password(user.getPassword())
                        .fullName(user.getFullName())
                        .enabled(false)
                        .build()))
                .map(u -> {
                    u.setPassword(passwordEncoder.encode(u.getPassword()));
                    return u;
                })
                .flatMap(userRepository::save)
                .map(u -> {
                    u.setPassword(null);
                    return u;
                });
    }
}
