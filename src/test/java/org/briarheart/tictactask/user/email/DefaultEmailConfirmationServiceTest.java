package org.briarheart.tictactask.user.email;

import org.briarheart.tictactask.config.ApplicationProperties;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.email.EmailService;
import org.briarheart.tictactask.user.TokenExpiredException;
import org.briarheart.tictactask.user.UnableToSendMessageException;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.user.UserRepository;
import org.briarheart.tictactask.util.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailSendException;
import reactor.core.publisher.Mono;

import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultEmailConfirmationServiceTest {
    private DefaultEmailConfirmationService service;
    private EmailConfirmationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(EmailConfirmationTokenRepository.class);
        when(tokenRepository.save(any(EmailConfirmationToken.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(new User(args.getArgument(0))));

        ApplicationProperties appProps = new ApplicationProperties();
        appProps.setName("Horns and hooves");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        emailService = mock(EmailService.class);
        service = new DefaultEmailConfirmationService(tokenRepository, userRepository, appProps, messages, emailService);
    }

    @Test
    void shouldSendEmailConfirmationLinkToUser() {
        User user = User.builder().email("alice@mail.com").emailConfirmed(true).enabled(true).fullName("Alice").build();
        service.sendEmailConfirmationLink(user, Locale.ENGLISH).block();
        verify(emailService, times(1)).sendEmail(eq(user.getEmail()), anyString(), anyString());
    }

    @Test
    void shouldIncludeEmailConfirmationLinkIntoMessageText() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .fullName("Alice")
                .build();
        EmailConfirmationToken token = service.sendEmailConfirmationLink(user, Locale.ENGLISH).block();
        assertNotNull(token);

        ArgumentCaptor<String> emailTextCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(1)).sendEmail(eq(user.getEmail()), anyString(), emailTextCaptor.capture());
        String emailText = emailTextCaptor.getValue();
        String confirmationLink = "http://localhost:4200/account/email/confirmation?userId=" + user.getId()
                + "&token=" + token.getTokenValue();
        assertThat(emailText, containsString(confirmationLink));
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendEmailConfirmationLink(null, Locale.ENGLISH).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnEmailConfirmationLinkSendWhenMailExceptionIsThrown() {
        User user = User.builder().email("alice@mail.com").emailConfirmed(true).enabled(true).fullName("Alice").build();
        doThrow(new MailSendException("Something went wrong")).when(emailService)
                .sendEmail(eq(user.getEmail()), anyString(), anyString());
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
                .expiresAt(DateTimeUtils.currentDateTimeUtc().plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(user.getId(), token.getTokenValue()))
                .thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        service.confirmEmail(user.getId(), token.getTokenValue()).block();

        User expectedUser = new User(user);
        expectedUser.setEmailConfirmed(true);
        expectedUser.setEnabled(true);
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
                .expiresAt(DateTimeUtils.currentDateTimeUtc().minus(1, ChronoUnit.HOURS))
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
                .expiresAt(DateTimeUtils.currentDateTimeUtc().plus(1, ChronoUnit.HOURS))
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
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        EmailConfirmationToken token = EmailConfirmationToken.builder()
                .id(2L)
                .userId(user.getId())
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(DateTimeUtils.currentDateTimeUtc().plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(user.getId(), token.getTokenValue()))
                .thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        service.confirmEmail(user.getId(), token.getTokenValue()).block();
        verify(userRepository, never()).save(any(User.class));
    }
}
