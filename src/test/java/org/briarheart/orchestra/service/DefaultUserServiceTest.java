package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultUserServiceTest {
    private DefaultUserService service;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        Map<String, PasswordEncoder> passwordEncoders = Map.of("bcrypt", new BCryptPasswordEncoder());
        PasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("bcrypt", passwordEncoders);
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
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.getPassword().startsWith("{bcrypt}"));
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
