package org.briarheart.orchestra.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
 * Default implementation of {@link EmailConfirmationService}.
 *
 * @author Roman Chigvintsev
 */
@Component
@RequiredArgsConstructor
public class DefaultEmailConfirmationService implements EmailConfirmationService {
    private static final Duration DEFAULT_EMAIL_CONFIRMATION_TOKEN_EXPIRATION_TIMEOUT = Duration.of(24, ChronoUnit.HOURS);

    @NonNull
    private final EmailConfirmationTokenRepository tokenRepository;
    @NonNull
    private final UserRepository userRepository;
    @NonNull
    private final ApplicationInfoProperties applicationInfo;
    @NonNull
    private final MessageSourceAccessor messages;
    @NonNull
    private final JavaMailSender mailSender;

    @Setter
    private Duration emailConfirmationTokenExpirationTimeout = DEFAULT_EMAIL_CONFIRMATION_TOKEN_EXPIRATION_TIMEOUT;

    @Override
    public Mono<EmailConfirmationToken> sendEmailConfirmationLink(User user, Locale locale)
            throws UnableToSendMessageException {
        Assert.notNull(user, "User must not be null");

        return createEmailConfirmationToken(user)
                .map(token -> {
                    String confirmationLink = UriComponentsBuilder.fromHttpUrl(applicationInfo.getUrl())
                            .path("/user/email/confirmation")
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
                    return token;
                })
                .onErrorMap(MailException.class, e -> {
                    String message = messages.getMessage(
                            "user.registration.email-confirmation.link.unable-to-send",
                            new Object[]{user.getEmail()},
                            Locale.ENGLISH
                    );
                    String localizedMessage;
                    if (locale == Locale.ENGLISH) {
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
                .switchIfEmpty(Mono.error(new EmailConfirmationTokenExpiredException("Email confirmation token \""
                        + tokenValue + "\" is expired")))
                .flatMap(token -> userRepository.findById(userId))
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + userId + " is not found")))
                .filter(user -> !user.isEmailConfirmed())
                .flatMap(user -> {
                    user.setEmailConfirmed(true);
                    user.setEnabled(true);
                    user.setVersion(user.getVersion() + 1);
                    return userRepository.save(user);
                })
                .then();
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
