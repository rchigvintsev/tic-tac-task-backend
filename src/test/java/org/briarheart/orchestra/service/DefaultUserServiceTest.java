package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.PasswordResetToken;
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
    private EmailConfirmationService emailConfirmationService;
    private PasswordService passwordService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(new User(args.getArgument(0))));

        emailConfirmationService = mock(EmailConfirmationService.class);
        when(emailConfirmationService.sendEmailConfirmationLink(any(User.class), eq(Locale.ENGLISH)))
                .thenAnswer(args -> {
                    User user = args.getArgument(0);
                    EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                            .id(1L)
                            .userId(user.getId())
                            .email(user.getEmail())
                            .tokenValue("K1Mb2ByFcfYndPmuFijB")
                            .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                            .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(24, ChronoUnit.HOURS))
                            .build();
                    return Mono.just(emailConfirmationToken);
                });

        passwordService = mock(PasswordService.class);
        when(passwordService.sendPasswordResetLink(any(User.class), eq(Locale.ENGLISH))).thenAnswer(args -> {
            User user = args.getArgument(0);
            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .id(1L)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .tokenValue("K1Mb2ByFcfYndPmuFijB")
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(24, ChronoUnit.HOURS))
                    .build();
            return Mono.just(passwordResetToken);
        });

        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(args -> args.getArgument(0));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        MessageSourceAccessor messages = new MessageSourceAccessor(messageSource);

        service = new DefaultUserService(userRepository, emailConfirmationService, passwordService, passwordEncoder,
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
    void shouldSendEmailConfirmationLinkToUserOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser, Locale.ENGLISH).block();
        verify(emailConfirmationService, times(1)).sendEmailConfirmationLink(any(User.class), eq(Locale.ENGLISH));
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
    void shouldSendPasswordResetLinkToUserOnPasswordReset() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmailAndEnabled(user.getEmail(), true)).thenReturn(Mono.just(user));
        service.resetPassword(user.getEmail(), Locale.ENGLISH).block();
        verify(passwordService, times(1)).sendPasswordResetLink(user, Locale.ENGLISH);
    }

    @Test
    void shouldDoNothingOnPasswordResetWhenUserIsNotFound() {
        String email = "alice@mail.com";
        when(userRepository.findByEmailAndEnabled(email, true)).thenReturn(Mono.empty());
        service.resetPassword(email, Locale.ENGLISH).block();
        verify(passwordService, never()).sendPasswordResetLink(any(User.class), any(Locale.class));
    }

    @Test
    void shouldThrowExceptionOnPasswordResetWhenEmailIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword(null, Locale.ENGLISH).block());
        assertEquals("Email address must not be null or empty", e.getMessage());
    }
}
