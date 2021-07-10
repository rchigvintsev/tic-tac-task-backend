package org.briarheart.orchestra.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.PasswordResetConfirmationTokenRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.PasswordResetConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@Slf4j
public class DefaultPasswordService implements PasswordService {
    private static final Duration DEFAULT_TOKEN_EXPIRATION_TIMEOUT = Duration.of(24, ChronoUnit.HOURS);

    private final PasswordResetConfirmationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationInfoProperties applicationInfo;
    private final MessageSourceAccessor messages;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Setter
    private Duration passwordResetTokenExpirationTimeout = DEFAULT_TOKEN_EXPIRATION_TIMEOUT;

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
    public Mono<Void> resetPassword(String email, Locale locale) {
        Assert.hasLength(email, "Email address must not be null or empty");
        return userRepository.findByEmailAndEnabled(email, true)
                .flatMap(user -> sendPasswordResetLink(user, locale))
                .then();
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
                .flatMap(token -> findUser(userId))
                .flatMap(user -> {
                    user.setPassword(encodePassword(newPassword));
                    user.setVersion(user.getVersion() + 1);
                    return userRepository.save(user)
                            .doOnSuccess(u -> log.debug("Password is reset for user with id {}", u.getId()));
                })
                .then();
    }

    @Override
    public Mono<Void> changePassword(Long userId, String currentPassword, String newPassword) {
        return findUser(userId)
                .flatMap(user -> {
                    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                        return Mono.error(new InvalidPasswordException());
                    }
                    return Mono.just(user);
                }).map(user -> {
                    user.setPassword(encodePassword(newPassword));
                    user.setVersion(user.getVersion() + 1);
                    return userRepository.save(user);
                }).then();
    }

    private Mono<PasswordResetConfirmationToken> sendPasswordResetLink(User user, Locale locale)
            throws UnableToSendMessageException {
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

                    String linkExpiresAfter = formatPasswordResetTokenExpirationTimeout(locale);
                    String text = messages.getMessage("user.password-reset.message.text",
                            new Object[]{user.getFullName(), passwordResetLink, linkExpiresAfter}, locale);

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

    private String formatPasswordResetTokenExpirationTimeout(Locale locale) {
        Period period = new Period(passwordResetTokenExpirationTimeout.toMillis());
        PeriodFormatter periodFormatter = PeriodFormat.wordBased(locale);
        return periodFormatter.print(period.normalizedStandard(PeriodType.dayTime()));
    }

    private Mono<User> findUser(Long id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + id + " is not found")));
    }
}
