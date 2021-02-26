package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.briarheart.orchestra.data.EmailConfirmationTokenRepository;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
    private final EmailConfirmationTokenRepository tokenRepository;
    private final EmailConfirmationLinkSender emailConfirmationLinkSender;
    private final PasswordEncoder passwordEncoder;
    private final MessageSourceAccessor messages;

    @Setter
    private Duration emailConfirmationTokenExpirationTimeout = DEFAULT_EMAIL_CONFIRMATION_TOKEN_EXPIRATION_TIMEOUT;

    @Override
    public Mono<User> createUser(User user, Locale locale) throws EntityAlreadyExistsException {
        Assert.notNull(user, "User must not be null");

        String email = user.getEmail();
        return userRepository.findByEmail(email)
                .flatMap(u -> {
                    String errorMessage = messages.getMessage("user.registration.user-already-registered",
                            new Object[]{email}, locale);
                    return Mono.<User>error(new EntityAlreadyExistsException(errorMessage));
                })
                .switchIfEmpty(Mono.fromCallable(() -> User.builder()
                        .email(email)
                        .emailConfirmed(false)
                        .password(user.getPassword())
                        .fullName(user.getFullName())
                        .enabled(false)
                        .build()))
                .map(this::encodePassword)
                .flatMap(userRepository::save)
                .map(this::clearPassword)
                .zipWhen(this::createEmailConfirmationToken)
                .flatMap(userAndToken -> {
                    EmailConfirmationToken token = userAndToken.getT2();
                    User savedUser = userAndToken.getT1();
                    return emailConfirmationLinkSender.sendEmailConfirmationLink(savedUser, token, locale)
                            .thenReturn(savedUser);
                });
    }

    @Override
    public Mono<Void> confirmEmail(Long userId, String tokenValue) throws EntityNotFoundException {
        return tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(userId, tokenValue)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Email confirmation token \""
                        + tokenValue + "\" is not registered for user with id " + userId)))
                .filter(token -> !token.isExpired())
                .switchIfEmpty(Mono.error(new EmailConfirmationTokenExpiredException("Email confirmation token \""
                        + tokenValue + "\" is expired")))
                .flatMap(token -> userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + userId + " is not found")))
                .filter(user -> !user.isEmailConfirmed())
                .map(user -> {
                    user.setEmailConfirmed(true);
                    user.setEnabled(true);
                    return userRepository.save(user);
                })
                .then();
    }

    private User encodePassword(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return user;
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
