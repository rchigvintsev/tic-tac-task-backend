package org.briarheart.orchestra.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class DefaultPasswordService implements PasswordService {
    private static final Duration DEFAULT_PASSWORD_RESET_TOKEN_EXPIRATION_TIMEOUT = Duration.of(24, ChronoUnit.HOURS);

    private final PasswordResetConfirmationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationInfoProperties applicationInfo;
    private final MessageSourceAccessor messages;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Setter
    private Duration passwordResetTokenExpirationTimeout = DEFAULT_PASSWORD_RESET_TOKEN_EXPIRATION_TIMEOUT;

    public DefaultPasswordService(PasswordResetConfirmationTokenRepository tokenRepository,
                                  UserRepository userRepository,
                                  ApplicationInfoProperties applicationInfo,
                                  MessageSourceAccessor messages,
                                  JavaMailSender mailSender,
                                  PasswordEncoder passwordEncoder) {
        Assert.notNull(tokenRepository, "Token repository must not be null");
        Assert.notNull(userRepository, "User repository must not be null");
        Assert.notNull(applicationInfo, "Application info properties must not be null");
        Assert.notNull(messages, "Message source accessor must not be null");
        Assert.notNull(mailSender, "Mail sender must not be null");
        Assert.notNull(passwordEncoder, "Password encoder must not be null");

        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.applicationInfo = applicationInfo;
        this.messages = messages;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

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
                    log.debug("Password reset confirmation link is sent to email {}", user.getEmail());
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
        return tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(userId, tokenValue, true)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Password reset confirmation token \""
                        + tokenValue + "\" is not registered for user with id " + userId)))
                .filter(token -> !token.isExpired())
                .switchIfEmpty(Mono.error(new TokenExpiredException("Password reset confirmation token \""
                        + tokenValue + "\" is expired")))
                .flatMap(token -> {
                    token.setValid(false);
                    return tokenRepository.save(token)
                            .doOnSuccess(t -> log.debug("Token with id {} is marked as invalid", t.getId()));
                })
                .flatMap(token -> userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + userId + " is not found")))
                .flatMap(user -> {
                    user.setPassword(encodePassword(newPassword));
                    user.setVersion(user.getVersion() + 1);
                    return userRepository.save(user)
                            .doOnSuccess(u -> log.debug("Password is reset for user with id {}", u.getId()));
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
        return tokenRepository.save(token)
                .doOnSuccess(t -> log.debug("Password reset confirmation token with id {} is created", t.getId()));
    }

    private String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
