package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.event.UserCreateEvent;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Mono<User> createUser(User user) throws EntityAlreadyExistsException {
        Assert.notNull(user, "User must not be null");
        return userRepository.findById(user.getEmail())
                .flatMap(u -> {
                    String errorMessage = "User with email \"" + user.getEmail() + "\" is already registered";
                    return Mono.<User>error(new EntityAlreadyExistsException(errorMessage));
                })
                .switchIfEmpty(Mono.fromCallable(() -> User.builder()
                        .email(user.getEmail())
                        .emailConfirmed(false)
                        .password(user.getPassword())
                        .fullName(user.getFullName())
                        .enabled(false)
                        .build()))
                .map(this::encodePassword)
                .flatMap(userRepository::save)
                .map(this::clearPassword)
                .map(this::publishUserCreateEvent);
    }

    private User encodePassword(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return user;
    }

    private User clearPassword(User user) {
        user.setPassword(null);
        return user;
    }

    private User publishUserCreateEvent(User user) {
        eventPublisher.publishEvent(new UserCreateEvent(user));
        return user;
    }
}
