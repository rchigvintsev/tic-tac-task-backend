package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EmailConfirmationTokenRepository;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

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
    private EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private EmailConfirmationLinkSender emailConfirmationLinkSender;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        emailConfirmationTokenRepository = mock(EmailConfirmationTokenRepository.class);
        when(emailConfirmationTokenRepository.save(any(EmailConfirmationToken.class)))
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

        service = new DefaultUserService(userRepository, emailConfirmationTokenRepository, emailConfirmationLinkSender,
                passwordEncoder, messages);
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
        verify(emailConfirmationTokenRepository, times(1)).save(any(EmailConfirmationToken.class));
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
        assertThrows(IllegalArgumentException.class, () -> service.createUser(null, Locale.ENGLISH),
                "User must not be null");
    }

    @Test
    void shouldThrowExceptionOnUserCreateWhenUserAlreadyExists() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        assertThrows(EntityAlreadyExistsException.class, () -> service.createUser(user, Locale.ENGLISH).block(),
                "User with email \"" + user.getEmail() + "\" is already registered");
    }
}
