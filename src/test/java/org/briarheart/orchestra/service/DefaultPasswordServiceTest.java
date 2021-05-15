package org.briarheart.orchestra.service;

import org.briarheart.orchestra.config.ApplicationInfoProperties;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.PasswordResetConfirmationTokenRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.PasswordResetConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultPasswordServiceTest {
    private static final String APPLICATION_URL = "https://horns-and-hooves.com";

    private DefaultPasswordService service;
    private PasswordResetConfirmationTokenRepository tokenRepository;
    private UserRepository userRepository;
    private JavaMailSender javaMailSender;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        tokenRepository = mock(PasswordResetConfirmationTokenRepository.class);
        when(tokenRepository.save(any(PasswordResetConfirmationToken.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        ApplicationInfoProperties appInfo = new ApplicationInfoProperties();
        appInfo.setName("Horns and hooves");
        appInfo.setUrl(APPLICATION_URL);

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        javaMailSender = mock(JavaMailSender.class);

        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(args -> args.getArgument(0));

        service = new DefaultPasswordService(tokenRepository, userRepository, appInfo, messages, javaMailSender,
                passwordEncoder);
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
        PasswordResetConfirmationToken token = service.sendPasswordResetLink(user, Locale.ENGLISH).block();
        assertNotNull(token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        String confirmationLink = APPLICATION_URL + "/account/password/reset/confirmation?userId=" + user.getId()
                + "&token=" + token.getTokenValue();
        assertThat(message.getText(), containsString(confirmationLink));
    }

    @Test
    void shouldIncludeLinkExpirationTimoutIntoMessageText() {
        User user = User.builder().id(1L).email("alice@mail.com").fullName("Alice").build();
        PasswordResetConfirmationToken token = service.sendPasswordResetLink(user, Locale.ENGLISH).block();
        assertNotNull(token);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getText(), containsString("1 day"));
    }

    @Test
    void shouldThrowExceptionOnPasswordResetLinkSendWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.sendPasswordResetLink(null, Locale.ENGLISH).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnPasswordResetLinkSendWhenMailExceptionIsThrown() {
        User user = User.builder().email("alice@mail.com").fullName("Alice").build();
        doThrow(new MailSendException("Something went wrong")).when(javaMailSender).send(any(SimpleMailMessage.class));
        assertThrows(UnableToSendMessageException.class,
                () -> service.sendPasswordResetLink(user, Locale.ENGLISH).block());
    }

    @Test
    void shouldConfirmPasswordReset() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        PasswordResetConfirmationToken token = PasswordResetConfirmationToken.builder()
                .id(2L)
                .userId(user.getId())
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(
                user.getId(),
                token.getTokenValue(),
                true
        )).thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        String newPassword = "qwerty";
        service.confirmPasswordReset(user.getId(), token.getTokenValue(), newPassword).block();

        User expectedUser = new User(user);
        expectedUser.setPassword(newPassword);
        expectedUser.setVersion(1L);
        verify(userRepository, times(1)).save(expectedUser);
    }

    @Test
    void shouldInvalidateTokenOnPasswordResetConfirm() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        PasswordResetConfirmationToken token = PasswordResetConfirmationToken.builder()
                .id(2L)
                .userId(user.getId())
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(
                user.getId(),
                token.getTokenValue(),
                true
        )).thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        String newPassword = "qwerty";
        service.confirmPasswordReset(user.getId(), token.getTokenValue(), newPassword).block();

        PasswordResetConfirmationToken invalidatedToken = new PasswordResetConfirmationToken(token);
        invalidatedToken.setValid(false);
        verify(tokenRepository, times(1)).save(invalidatedToken);
    }

    @Test
    void shouldEncodePasswordOnPasswordResetConfirm() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        PasswordResetConfirmationToken token = PasswordResetConfirmationToken.builder()
                .id(2L)
                .userId(user.getId())
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();

        when(tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(
                user.getId(),
                token.getTokenValue(),
                true
        )).thenReturn(Mono.just(token));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        String newPassword = "qwerty";
        service.confirmPasswordReset(user.getId(), token.getTokenValue(), newPassword).block();
        verify(passwordEncoder, times(1)).encode(newPassword);
    }

    @Test
    void shouldThrowExceptionOnPasswordResetConfirmWhenTokenIsNotFound() {
        long userId = 1L;
        String tokenValue = "K1Mb2ByFcfYndPmuFijB";
        when(tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(userId, tokenValue, true))
                .thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.confirmPasswordReset(userId, tokenValue, "qwerty").block());
        assertEquals("Password reset confirmation token \"" + tokenValue + "\" is not registered for user with id "
                        + userId, e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnPasswordResetConfirmWhenTokenIsExpired() {
        long userId = 1L;
        PasswordResetConfirmationToken token = PasswordResetConfirmationToken.builder()
                .id(2L)
                .userId(userId)
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).minus(1, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(
                userId,
                token.getTokenValue(),
                true
        )).thenReturn(Mono.just(token));
        TokenExpiredException e = assertThrows(TokenExpiredException.class,
                () -> service.confirmPasswordReset(userId, token.getTokenValue(), "qwerty").block());
        assertEquals("Password reset confirmation token \"" + token.getTokenValue() + "\" is expired", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnPasswordResetConfirmWhenUserIsNotFound() {
        long userId = 1L;
        PasswordResetConfirmationToken token = PasswordResetConfirmationToken.builder()
                .id(2L)
                .userId(userId)
                .tokenValue("K1Mb2ByFcfYndPmuFijB")
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(
                userId,
                token.getTokenValue(),
                true
        )).thenReturn(Mono.just(token));
        when(userRepository.findById(userId)).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.confirmPasswordReset(userId, token.getTokenValue(), "qwerty").block());
        assertEquals("User with id " + userId + " is not found", e.getMessage());
    }
}
