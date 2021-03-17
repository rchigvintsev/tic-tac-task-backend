package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Locale;

/**
 * Default implementation of {@link UserService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {
    @NonNull
    private final UserRepository userRepository;
    @NonNull
    private final EmailConfirmationService emailConfirmationService;
    @NonNull
    private final PasswordService passwordService;
    @NonNull
    private final PasswordEncoder passwordEncoder;
    @NonNull
    private final MessageSourceAccessor messages;

    @Override
    public Mono<User> createUser(User user, Locale locale) throws EntityAlreadyExistsException {
        Assert.notNull(user, "User must not be null");

        String email = user.getEmail();
        return userRepository.findByEmail(email)
                .flatMap(u -> ensureEmailNotConfirmed(u, locale))
                .flatMap(u -> {
                    u.setFullName(user.getFullName());
                    u.setPassword(encodePassword(user.getPassword()));
                    return userRepository.save(u);
                })
                .switchIfEmpty(Mono.defer(() -> createNewUser(user)))
                .map(this::clearPassword)
                .flatMap(u -> emailConfirmationService.sendEmailConfirmationLink(u, locale).map(token -> u));
    }

    @Override
    public Mono<Void> resetPassword(String email, Locale locale) {
        Assert.hasLength(email, "Email address must not be null or empty");
        return userRepository.findByEmailAndEnabled(email, true)
                .flatMap(user -> passwordService.sendPasswordResetLink(user, locale))
                .then();
    }

    private Mono<User> ensureEmailNotConfirmed(User user, Locale locale) {
        if (user.isEmailConfirmed()) {
            String message = messages.getMessage("user.registration.user-already-registered",
                    new Object[]{user.getEmail()}, Locale.ENGLISH);
            String localizedMessage;
            if (locale == null || locale == Locale.ENGLISH) {
                localizedMessage = message;
            } else {
                localizedMessage = messages.getMessage("user.registration.user-already-registered",
                        new Object[]{user.getEmail()}, locale);
            }
            return Mono.error(new EntityAlreadyExistsException(message, localizedMessage));
        }
        return Mono.just(user);
    }

    private Mono<User> createNewUser(User user) {
        User newUser = new User(user);
        newUser.setId(null);
        newUser.setEmailConfirmed(false);
        newUser.setVersion(0L);
        newUser.setPassword(encodePassword(user.getPassword()));
        newUser.setEnabled(false);
        newUser.setAuthorities(Collections.emptySet());
        return userRepository.save(newUser);
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    private User clearPassword(User user) {
        user.setPassword(null);
        return user;
    }
}
