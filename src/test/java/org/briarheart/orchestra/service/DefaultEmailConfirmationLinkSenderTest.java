package org.briarheart.orchestra.service;

import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultEmailConfirmationLinkSenderTest {
    private static final String APPLICATION_URL = "https://horns-and-hooves.com";

    private DefaultEmailConfirmationLinkSender sender;
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        ApplicationInfoProperties appInfo = new ApplicationInfoProperties();
        appInfo.setName("Horns and hooves");
        appInfo.setUrl(APPLICATION_URL);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        javaMailSender = mock(JavaMailSender.class);
        sender = new DefaultEmailConfirmationLinkSender(appInfo, messages, javaMailSender);
    }

    @Test
    void shouldSendEmailConfirmationLinkToUser() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .email(user.getEmail())
                .tokenValue(UUID.randomUUID().toString())
                .build();

        sender.sendEmailConfirmationLink(user, token, Locale.ENGLISH).block();
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo(), arrayContaining(user.getEmail()));
    }

    @Test
    void shouldIncludeConfirmationLinkIntoMessageText() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .email(user.getEmail())
                .tokenValue(UUID.randomUUID().toString())
                .build();
        String confirmationLink = APPLICATION_URL + "?token=" + token.getTokenValue();

        sender.sendEmailConfirmationLink(user, token, Locale.ENGLISH).block();
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getText(), containsString(confirmationLink));
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenUserIsNull() {
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .email("alice@mail.com")
                .tokenValue(UUID.randomUUID().toString())
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> sender.sendEmailConfirmationLink(null, token, Locale.ENGLISH),
                "User must not be null");
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenEmailConfirmationTokenIsNull() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        assertThrows(IllegalArgumentException.class, () -> sender.sendEmailConfirmationLink(user, null, Locale.ENGLISH),
                "Email confirmation token must not be null");
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenMailExceptionIsThrown() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .email(user.getEmail())
                .tokenValue(UUID.randomUUID().toString())
                .build();
        doThrow(new MailSendException("Something went wrong")).when(javaMailSender).send(any(SimpleMailMessage.class));
        assertThrows(UnableToSendMessageException.class,
                () -> sender.sendEmailConfirmationLink(user, token, Locale.ENGLISH).block());
    }
}
