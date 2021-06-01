package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
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
@Slf4j
public class DefaultUserService implements UserService {
    private final UserRepository userRepository;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordService passwordService;
    private final PasswordEncoder passwordEncoder;
    private final MessageSourceAccessor messages;

    public DefaultUserService(UserRepository userRepository,
                              EmailConfirmationService emailConfirmationService,
                              PasswordService passwordService,
                              PasswordEncoder passwordEncoder,
                              MessageSourceAccessor messages) {
        Assert.notNull(userRepository, "User repository must not be null");
        Assert.notNull(emailConfirmationService, "Email confirmation service must not be null");
        Assert.notNull(passwordService, "Password service must not be null");
        Assert.notNull(passwordEncoder, "Password encoder must not be null");
        Assert.notNull(messages, "Message source accessor must not be null");

        this.userRepository = userRepository;
        this.emailConfirmationService = emailConfirmationService;
        this.passwordService = passwordService;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
    }

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
                .switchIfEmpty(Mono.defer(() -> createNewUser(user)
                        .doOnSuccess(u -> log.debug("User with id {} is created", u.getId()))))
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

    @Override
    public Mono<User> updateUser(User user) {
        Assert.notNull(user, "User must not be null");
        return findUser(user.getId(), user.getEmail()).flatMap(existingUser -> {
            User updatedUser = new User(user);
            updatedUser.setEmailConfirmed(existingUser.isEmailConfirmed());
            updatedUser.setVersion(existingUser.getVersion() + 1);
            updatedUser.setPassword(existingUser.getPassword());
            updatedUser.setEnabled(existingUser.isEnabled());
            return userRepository.save(updatedUser)
                    .map(this::clearPassword)
                    .doOnSuccess(u -> log.debug("User with id {} is updated", u.getId()));
        });
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
        return rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
    }

    private User clearPassword(User user) {
        user.setPassword(null);
        return user;
    }

    private Mono<User> findUser(Long id, String email) {
        return userRepository.findByIdAndEmail(id, email)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + id + " is not found")));
    }
}
