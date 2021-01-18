package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultUserServiceTest {
    private DefaultUserService service;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(args -> args.getArgument(0));
        service = new DefaultUserService(userRepository, passwordEncoder);
    }

    @Test
    void shouldCreateUser() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findById(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser).block();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldEncodePasswordOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findById(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser).block();
        verify(passwordEncoder, times(1)).encode(newUser.getPassword());
    }

    @Test
    void shouldClearPasswordOnUserCreateWhenUserIsSaved() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findById(newUser.getEmail())).thenReturn(Mono.empty());
        User result = service.createUser(newUser).block();
        assertNotNull(result);
        assertNull(result.getPassword());
    }

    @Test
    void shouldThrowExceptionOnUserCreateWhenUserIsNull() {
        assertThrows(IllegalArgumentException.class, () -> service.createUser(null), "User must not be null");
    }

    @Test
    void shouldThrowExceptionOnUserCreateWhenUserAlreadyExists() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findById(user.getEmail())).thenReturn(Mono.just(user));
        assertThrows(EntityAlreadyExistsException.class, () -> service.createUser(user).block(),
                "User with email \"" + user.getEmail() + "\" is already registered");
    }
}
