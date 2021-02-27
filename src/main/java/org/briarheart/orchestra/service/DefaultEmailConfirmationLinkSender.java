package org.briarheart.orchestra.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.config.ApplicationInfoProperties;
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

import java.util.Locale;

/**
 * @author Roman Chigvintsev
 */
@Component
@RequiredArgsConstructor
public class DefaultEmailConfirmationLinkSender implements EmailConfirmationLinkSender {
    @NonNull
    private final ApplicationInfoProperties applicationInfo;
    @NonNull
    private final MessageSourceAccessor messages;
    @NonNull
    private final JavaMailSender mailSender;

    @Override
    public Mono<Void> sendEmailConfirmationLink(User user, EmailConfirmationToken token, Locale locale)
            throws UnableToSendMessageException {
        Assert.notNull(user, "User must not be null");
        Assert.notNull(token, "Email confirmation token must not be null");

        return Mono.fromRunnable(() -> {
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
        }).onErrorMap(MailException.class, e -> {
            String errorMessage = messages.getMessage("user.registration.email-confirmation.link.unable-to-send",
                    new Object[]{user.getEmail()}, locale);
            return new UnableToSendMessageException(errorMessage, e);
        }).then();
    }
}
