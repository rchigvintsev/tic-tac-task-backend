package org.briarheart.orchestra.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.PasswordResetConfirmationTokenRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.PasswordResetConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

/**
 * Default implementation of {@link PasswordService}.
 *
 * @author Roman Chigvintsev
 */
@Component
@RequiredArgsConstructor
public class DefaultPasswordService implements PasswordService {
    private static final Duration DEFAULT_PASSWORD_RESET_TOKEN_EXPIRATION_TIMEOUT = Duration.of(24, ChronoUnit.HOURS);

    @NonNull
    private final PasswordResetConfirmationTokenRepository tokenRepository;
    @NonNull
    private final UserRepository userRepository;
    @NonNull
    private final ApplicationInfoProperties applicationInfo;
    @NonNull
    private final MessageSourceAccessor messages;
    @NonNull
    private final JavaMailSender mailSender;
    @NonNull
    private final PasswordEncoder passwordEncoder;

    @Setter
    private Duration passwordResetTokenExpirationTimeout = DEFAULT_PASSWORD_RESET_TOKEN_EXPIRATION_TIMEOUT;

    @Override
    public Mono<PasswordResetConfirmationToken> sendPasswordResetLink(User user, Locale locale)
            throws UnableToSendMessageException {
        Assert.notNull(user, "User must not be null");

        return createPasswordResetToken(user)
                .map(token -> {
                    String passwordResetLink = UriComponentsBuilder.fromHttpUrl(applicationInfo.getUrl())
                            .path("/account/password/reset/confirmation")
                            .queryParam("userId", user.getId())
                            .queryParam("token", token.getTokenValue())
                            .build()
                            .toUriString();

                    String subject = messages.getMessage("user.password-reset.message.subject",
                            new Object[]{applicationInfo.getName()}, locale);
                    String text = messages.getMessage("user.password-reset.message.text",
                            new Object[]{user.getFullName(), passwordResetLink}, locale);

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(user.getEmail());
                    message.setSubject(subject);
                    message.setText(text);
                    mailSender.send(message);
                    return token;
                })
                .onErrorMap(MailException.class, e -> {
                    String message = messages.getMessage("user.password-reset.link.unable-to-send",
                            new Object[]{user.getEmail()}, Locale.ENGLISH);
                    String localizedMessage;
                    if (locale == null || locale == Locale.ENGLISH) {
                        localizedMessage = message;
                    } else {
                        localizedMessage = messages.getMessage("user.password-reset.link.unable-to-send",
                                new Object[]{user.getEmail()}, locale);
                    }
                    return new UnableToSendMessageException(message, localizedMessage, e);
                });
    }

    @Override
    public Mono<Void> confirmPasswordReset(Long userId, String tokenValue, String newPassword)
            throws EntityNotFoundException, TokenExpiredException {
        return tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(userId, tokenValue)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Password reset confirmation token \""
                        + tokenValue + "\" is not registered for user with id " + userId)))
                .filter(token -> !token.isExpired())
                .switchIfEmpty(Mono.error(new TokenExpiredException("Password reset confirmation token \""
                        + tokenValue + "\" is expired")))
                .flatMap(token -> userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + userId + " is not found")))
                .flatMap(user -> {
                    user.setPassword(encodePassword(newPassword));
                    user.setVersion(user.getVersion() + 1);
                    return userRepository.save(user);
                })
                .then();
    }

    private Mono<PasswordResetConfirmationToken> createPasswordResetToken(User user) {
        LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiresAt = createdAt.plus(passwordResetTokenExpirationTimeout);
        PasswordResetConfirmationToken token = PasswordResetConfirmationToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .tokenValue(UUID.randomUUID().toString())
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
        return tokenRepository.save(token);
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
