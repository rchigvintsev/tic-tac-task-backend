package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

/**
 * Default implementation of {@link UserService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@RequiredArgsConstructor
public class DefaultUserService implements UserService {
    private static final Duration DEFAULT_EMAIL_CONFIRMATION_TOKEN_EXPIRATION_TIMEOUT = Duration.of(24, ChronoUnit.HOURS);

    private final UserRepository userRepository;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordEncoder passwordEncoder;
    private final MessageSourceAccessor messages;

    @Setter
    private Duration emailConfirmationTokenExpirationTimeout = DEFAULT_EMAIL_CONFIRMATION_TOKEN_EXPIRATION_TIMEOUT;

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
                .zipWhen(u -> emailConfirmationService.sendEmailConfirmationLink(u, locale))
                .map(Tuple2::getT1);
    }

    private Mono<User> ensureEmailNotConfirmed(User user, Locale locale) {
        if (user.isEmailConfirmed()) {
            String message = messages.getMessage("user.registration.user-already-registered",
                    new Object[]{user.getEmail()}, Locale.ENGLISH);
            String localizedMessage;
            if (locale == Locale.ENGLISH) {
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

    private Mono<EmailConfirmationToken> createEmailConfirmationToken(User user) {
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiresAt = createdAt.plus(emailConfirmationTokenExpirationTimeout);
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tokenValue(UUID.randomUUID().toString())
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
        return tokenRepository.save(token);
    }
}
