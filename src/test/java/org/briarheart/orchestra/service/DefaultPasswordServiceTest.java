package org.briarheart.orchestra.service;

import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.data.PasswordResetTokenRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.PasswordResetToken;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;

import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultPasswordServiceTest {
    private static final String APPLICATION_URL = "https://horns-and-hooves.com";

    private DefaultPasswordService service;
    private PasswordResetTokenRepository tokenRepository;
    private UserRepository userRepository;
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(PasswordResetTokenRepository.class);
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        ApplicationInfoProperties appInfo = new ApplicationInfoProperties();
        appInfo.setName("Horns and hooves");
        appInfo.setUrl(APPLICATION_URL);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        javaMailSender = mock(JavaMailSender.class);
        service = new DefaultPasswordService(tokenRepository, userRepository, appInfo, messages, javaMailSender);
    }

    @Test
    void shouldSendPasswordResetLinkToUser() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        service.sendPasswordResetLink(user, Locale.ENGLISH).block();
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo(), arrayContaining(user.getEmail()));
    }

    @Test
    void shouldIncludePasswordResetLinkIntoMessageText() {
        User user = User.builder().id(1L).email("alice@mail.com").fullName("Alice").build();
        PasswordResetToken token = service.sendPasswordResetLink(user, Locale.ENGLISH).block();
        assertNotNull(token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        String confirmationLink = APPLICATION_URL + "/user/password/reset?userId=" + user.getId()
                + "&token=" + token.getTokenValue();
        assertThat(message.getText(), containsString(confirmationLink));
    }

    @Test
    void shouldThrowExceptionOnPasswordResetLinkSendWhenUserIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.sendPasswordResetLink(null, Locale.ENGLISH).block(),
                "User must not be null");
    }

    @Test
    void shouldThrowExceptionOnPasswordResetLinkSendWhenMailExceptionIsThrown() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        doThrow(new MailSendException("Something went wrong")).when(javaMailSender).send(any(SimpleMailMessage.class));
        assertThrows(UnableToSendMessageException.class,
                () -> service.sendPasswordResetLink(user, Locale.ENGLISH).block());
    }
}
