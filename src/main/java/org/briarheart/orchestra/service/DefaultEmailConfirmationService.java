package org.briarheart.orchestra.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.data.EmailConfirmationTokenRepository;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
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
 * Default implementation of {@link EmailConfirmationService}.
 *
 * @author Roman Chigvintsev
 */
@Slf4j
public class DefaultEmailConfirmationService implements EmailConfirmationService {
    private static final Duration DEFAULT_TOKEN_EXPIRATION_TIMEOUT = Duration.of(24, ChronoUnit.HOURS);

    private final EmailConfirmationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ApplicationInfoProperties applicationInfo;
    private final MessageSourceAccessor messages;
    private final JavaMailSender mailSender;

    @Setter
    private Duration emailConfirmationTokenExpirationTimeout = DEFAULT_TOKEN_EXPIRATION_TIMEOUT;

    public DefaultEmailConfirmationService(EmailConfirmationTokenRepository tokenRepository,
                                           UserRepository userRepository,
                                           ApplicationInfoProperties applicationInfo,
                                           MessageSourceAccessor messages,
                                           JavaMailSender mailSender) {
        Assert.notNull(tokenRepository, "Token repository must not be null");
        Assert.notNull(userRepository, "User repository must not be null");
        Assert.notNull(applicationInfo, "Application info properties must not be null");
        Assert.notNull(messages, "Message source accessor must not be null");
        Assert.notNull(mailSender, "Mail sender must not be null");

        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.applicationInfo = applicationInfo;
        this.messages = messages;
        this.mailSender = mailSender;
    }

    @Transactional
    @Override
    public Mono<EmailConfirmationToken> sendEmailConfirmationLink(User user, Locale locale)
            throws UnableToSendMessageException {
        Assert.notNull(user, "User must not be null");
        return createEmailConfirmationToken(user)
                .map(token -> {
                    String confirmationLink = UriComponentsBuilder.fromHttpUrl(applicationInfo.getUrl())
                            .path("/account/email/confirmation")
                            .queryParam("userId", user.getId())
                            .queryParam("token", token.getTokenValue())
                            .build()
                            .toUriString();

                    String subject = messages.getMessage("user.registration.email-confirmation.message.subject",
                            new Object[]{applicationInfo.getName()}, locale);
                    String text = messages.getMessage("user.registration.email-confirmation.message.text",
                            new Object[]{user.getFullName(), applicationInfo.getName(), confirmationLink}, locale);

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(user.getEmail());
                    message.setSubject(subject);
                    message.setText(text);
                    mailSender.send(message);
                    log.debug("Email confirmation link is sent to email {}", user.getEmail());
                    return token;
                })
                .onErrorMap(MailException.class, e -> {
                    String message = messages.getMessage(
                            "user.registration.email-confirmation.link.unable-to-send",
                            new Object[]{user.getEmail()},
                            Locale.ENGLISH
                    );
                    String localizedMessage;
                    if (locale == null || locale == Locale.ENGLISH) {
                        localizedMessage = message;
                    } else {
                        localizedMessage = messages.getMessage(
                                "user.registration.email-confirmation.link.unable-to-send",
                                new Object[]{user.getEmail()},
                                locale
                        );
                    }
                    return new UnableToSendMessageException(message, localizedMessage, e);
                });
    }

    @Override
    public Mono<Void> confirmEmail(Long userId, String tokenValue) {
        return tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(userId, tokenValue)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Email confirmation token \""
                        + tokenValue + "\" is not registered for user with id " + userId)))
                .filter(token -> !token.isExpired())
                .switchIfEmpty(Mono.error(new TokenExpiredException("Email confirmation token \""
                        + tokenValue + "\" is expired")))
                .flatMap(token -> userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + userId + " is not found")))
                .filter(user -> !user.isEmailConfirmed())
                .flatMap(user -> {
                    user.setEmailConfirmed(true);
                    user.setEnabled(true);
                    return userRepository.save(user)
                            .doOnSuccess(u -> log.debug("Email {} is confirmed for user with id {}",
                                    user.getEmail(), user.getId()));
                })
                .then();
    }

    private Mono<EmailConfirmationToken> createEmailConfirmationToken(User user) {
        return Mono.defer(() -> {
            LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime expiresAt = createdAt.plus(emailConfirmationTokenExpirationTimeout);
            EmailConfirmationToken token = EmailConfirmationToken.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .tokenValue(UUID.randomUUID().toString())
                    .createdAt(createdAt)
                    .expiresAt(expiresAt)
                    .build();
            return tokenRepository.save(token)
                    .doOnSuccess(t -> log.debug("Email confirmation token with id {} is created", t.getId()));
        });
    }
}
