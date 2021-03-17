package org.briarheart.orchestra.service;

import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.data.EmailConfirmationTokenRepository;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.UserRepository;
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
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultEmailConfirmationServiceTest {
    private static final String APPLICATION_URL = "https://horns-and-hooves.com";

    private DefaultEmailConfirmationService service;
    private EmailConfirmationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(EmailConfirmationTokenRepository.class);
        when(tokenRepository.save(any(EmailConfirmationToken.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        ApplicationInfoProperties appInfo = new ApplicationInfoProperties();
        appInfo.setName("Horns and hooves");
        appInfo.setUrl(APPLICATION_URL);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        javaMailSender = mock(JavaMailSender.class);
        service = new DefaultEmailConfirmationService(tokenRepository, userRepository, appInfo, messages,
                javaMailSender);
    }

    @Test
    void shouldSendEmailConfirmationLinkToUser() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        service.sendEmailConfirmationLink(user, Locale.ENGLISH).block();
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo(), arrayContaining(user.getEmail()));
    }

    @Test
    void shouldIncludeEmailConfirmationLinkIntoMessageText() {
        User user = User.builder().id(1L).email("alice@mail.com").fullName("Alice").build();
        EmailConfirmationToken token = service.sendEmailConfirmationLink(user, Locale.ENGLISH).block();
        assertNotNull(token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        String confirmationLink = APPLICATION_URL + "/user/email/confirmation?userId=" + user.getId()
                + "&token=" + token.getTokenValue();
        assertThat(message.getText(), containsString(confirmationLink));
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendEmailConfirmationLink(null, Locale.ENGLISH).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenMailExceptionIsThrown() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        doThrow(new MailSendException("Something went wrong")).when(javaMailSender).send(any(SimpleMailMessage.class));
        assertThrows(UnableToSendMessageException.class,
                () -> service.sendEmailConfirmationLink(user, Locale.ENGLISH).block());
    }

    @Test
    void shouldConfirmEmail() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(false).enabled(false).build();
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .id(2L)
                .userId(user.getId())
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(user.getId(), token.getTokenValue()))
                .thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        service.confirmEmail(user.getId(), token.getTokenValue()).block();

        User expectedUser = new User(user);
        expectedUser.setEmailConfirmed(true);
        expectedUser.setEnabled(true);
        expectedUser.setVersion(1L);
        verify(userRepository, times(1)).save(expectedUser);
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmWhenTokenIsNotFound() {
        long userId = 1L;
        String tokenValue = "K1Mb2ByFcfYndPmuFijB";
        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(userId, tokenValue))
                .thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.confirmEmail(userId, tokenValue).block());
        assertEquals("Email confirmation token \"" + tokenValue + "\" is not registered for user with id " + userId,
                e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmWhenTokenIsExpired() {
        long userId = 1L;
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .id(2L)
                .userId(userId)
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).minus(1, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(userId, token.getTokenValue()))
                .thenReturn(Mono.just(token));
        TokenExpiredException e = assertThrows(TokenExpiredException.class,
                () -> service.confirmEmail(userId, token.getTokenValue()).block());
        assertEquals("Email confirmation token \"" + token.getTokenValue() + "\" is expired", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmWhenUserIsNotFound() {
        long userId = 1L;
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .id(2L)
                .userId(userId)
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(userId, token.getTokenValue()))
                .thenReturn(Mono.just(token));
        when(userRepository.findById(userId)).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.confirmEmail(userId, token.getTokenValue()).block());
        assertEquals("User with id " + userId + " is not found", e.getMessage());
    }

    @Test
    void shouldDoNothingOnEmailConfirmWhenEmailIsAlreadyConfirmed() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .id(2L)
                .userId(user.getId())
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(user.getId(), token.getTokenValue()))
                .thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        service.confirmEmail(user.getId(), token.getTokenValue()).block();
        verify(userRepository, never()).save(any(User.class));
    }
}
