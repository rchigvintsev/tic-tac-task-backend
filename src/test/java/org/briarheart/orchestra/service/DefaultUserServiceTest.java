package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EmailConfirmationTokenRepository;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultUserServiceTest {
    private DefaultUserService service;
    private UserRepository userRepository;
    private EmailConfirmationTokenRepository tokenRepository;
    private EmailConfirmationLinkSender emailConfirmationLinkSender;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(new User(args.getArgument(0))));

        tokenRepository = mock(EmailConfirmationTokenRepository.class);
        when(tokenRepository.save(any(EmailConfirmationToken.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        emailConfirmationLinkSender = mock(EmailConfirmationLinkSender.class);
        when(emailConfirmationLinkSender
                .sendEmailConfirmationLink(any(User.class), any(EmailConfirmationToken.class), eq(Locale.ENGLISH)))
                .thenReturn(Mono.just(true).then());

        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(args -> args.getArgument(0));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        service = new DefaultUserService(userRepository, tokenRepository, emailConfirmationLinkSender, passwordEncoder,
                messages);
    }

    @Test
    void shouldCreateUser() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser, Locale.ENGLISH).block();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldEncodePasswordOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser, Locale.ENGLISH).block();
        verify(passwordEncoder, times(1)).encode(newUser.getPassword());
    }

    @Test
    void shouldClearPasswordOnUserCreateWhenUserIsSaved() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        User result = service.createUser(newUser, Locale.ENGLISH).block();
        assertNotNull(result);
        assertNull(result.getPassword());
    }

    @Test
    void shouldDisableNewUserOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        User result = service.createUser(newUser, Locale.ENGLISH).block();
        assertNotNull(result);
        assertFalse(result.isEnabled());
    }

    @Test
    void shouldSetEmailConfirmationFlagToFalseOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        User result = service.createUser(newUser, Locale.ENGLISH).block();
        assertNotNull(result);
        assertFalse(result.isEmailConfirmed());
    }

    @Test
    void shouldCreateEmailConfirmationTokenOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser, Locale.ENGLISH).block();
        verify(tokenRepository, times(1)).save(any(EmailConfirmationToken.class));
    }

    @Test
    void shouldSendEmailConfirmationLinkToUserOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser, Locale.ENGLISH).block();
        verify(emailConfirmationLinkSender, times(1))
                .sendEmailConfirmationLink(any(User.class), any(EmailConfirmationToken.class), eq(Locale.ENGLISH));
    }

    @Test
    void shouldThrowExceptionOnUserCreateWhenUserIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.createUser(null, Locale.ENGLISH).block(),
                "User must not be null");
    }

    @Test
    void shouldThrowExceptionOnUserCreateWhenUserWithConfirmedEmailAlreadyExists() {
        User user = User.builder()
                .email("alice@mail.com")
                .emailConfirmed(true)
                .password("secret")
                .fullName("Alice")
                .enabled(true)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        assertThrows(EntityAlreadyExistsException.class, () -> service.createUser(user, Locale.ENGLISH).block(),
                "User with email \"" + user.getEmail() + "\" is already registered");
    }

    @Test
    void shouldUpdateFullNameOnUserCreateWhenUserWithUnconfirmedEmailAlreadyExists() {
        User user = User.builder()
                .email("alice@mail.com")
                .emailConfirmed(false)
                .password("secret")
                .fullName("Alice")
                .enabled(false)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));

        User newUser = new User(user);
        newUser.setFullName("Alice Wonderland");

        service.createUser(newUser, Locale.ENGLISH).block();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(newUser.getFullName(), userCaptor.getValue().getFullName());
    }

    @Test
    void shouldUpdatePasswordOnUserCreateWhenUserWithUnconfirmedEmailAlreadyExists() {
        User user = User.builder()
                .email("alice@mail.com")
                .emailConfirmed(false)
                .password("secret")
                .fullName("Alice")
                .enabled(false)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));

        User newUser = new User(user);
        newUser.setPassword("qwerty");

        service.createUser(newUser, Locale.ENGLISH).block();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(newUser.getPassword(), userCaptor.getValue().getPassword());
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
        assertThrows(EntityNotFoundException.class, () -> service.confirmEmail(userId, tokenValue).block(),
                "Email confirmation token \"" + tokenValue + "\" is not registered for user with id " + userId);
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
        assertThrows(EmailConfirmationTokenExpiredException.class,
                () -> service.confirmEmail(userId, token.getTokenValue()).block(),
                "Email confirmation token \"" + token.getTokenValue() + "\" is expired");
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
        assertThrows(EntityNotFoundException.class, () -> service.confirmEmail(userId, token.getTokenValue()).block(),
                "User with id " + userId + " is not found");
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
