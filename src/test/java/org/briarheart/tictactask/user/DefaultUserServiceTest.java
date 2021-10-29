package org.briarheart.tictactask.user;

import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.service.FileTooLargeException;
import org.briarheart.tictactask.user.email.EmailConfirmationService;
import org.briarheart.tictactask.user.email.EmailConfirmationToken;
import org.briarheart.tictactask.user.profilepicture.ProfilePicture;
import org.briarheart.tictactask.user.profilepicture.ProfilePictureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private ProfilePictureRepository profilePictureRepository;
    private EmailConfirmationService emailConfirmationService;
    private PasswordEncoder passwordEncoder;
    private MessageSourceAccessor messages;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(new User(args.getArgument(0))));

        profilePictureRepository = mock(ProfilePictureRepository.class);

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

        passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenAnswer(args -> args.getArgument(0));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messages = new MessageSourceAccessor(messageSource);

        service = new DefaultUserService(userRepository, profilePictureRepository, emailConfirmationService,
                passwordEncoder, messages);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenUserRepositoryIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new DefaultUserService(null,
                profilePictureRepository, emailConfirmationService, passwordEncoder, messages));
        assertEquals("User repository must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenProfilePictureRepositoryIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()
                -> new DefaultUserService(userRepository, null, emailConfirmationService, passwordEncoder, messages));
        assertEquals("Profile picture repository must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenEmailConfirmationServiceIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()
                -> new DefaultUserService(userRepository, profilePictureRepository, null, passwordEncoder, messages));
        assertEquals("Email confirmation service must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenPasswordEncoderIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()
                -> new DefaultUserService(userRepository, profilePictureRepository, emailConfirmationService, null,
                messages));
        assertEquals("Password encoder must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnConstructWhenMessageSourceAccessorIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, ()
                -> new DefaultUserService(userRepository, profilePictureRepository, emailConfirmationService,
                passwordEncoder, null));
        assertEquals("Message source accessor must not be null", e.getMessage());
    }

    @Test
    void shouldReturnNumberOfAllUsers() {
        long userCount = 3L;
        when(userRepository.count()).thenReturn(Mono.just(userCount));
        Long result = service.getUserCount().block();
        assertEquals(userCount, result);
    }

    @Test
    void shouldReturnAllUsers() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        when(userRepository.findAllOrderByIdAsc(0, null)).thenReturn(Flux.just(user));
        User result = service.getUsers(Pageable.unpaged()).blockFirst();
        assertEquals(user, result);
    }

    @Test
    void shouldReturnAllUsersWithPagingRestriction() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        PageRequest pageRequest = PageRequest.of(3, 50);
        when(userRepository.findAllOrderByIdAsc(pageRequest.getOffset(), pageRequest.getPageSize()))
                .thenReturn(Flux.just(user));
        User result = service.getUsers(pageRequest).blockFirst();
        assertEquals(user, result);
    }

    @Test
    void shouldCreateUser() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());
        service.createUser(newUser, Locale.ENGLISH).block();
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void shouldNotAllowToSetAdminFieldOnUserCreate() {
        User newUser = User.builder().email("alice@mail.com").password("secret").fullName("Alice").admin(true).build();
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Mono.empty());

        User result = service.createUser(newUser, Locale.ENGLISH).block();
        assertNotNull(result);
        assertFalse(result.isAdmin());
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
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.createUser(null, Locale.ENGLISH).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnUserCreateWhenUserWithConfirmedEmailAlreadyExists() {
        User user = User.builder()
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .fullName("Alice")
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));
        EntityAlreadyExistsException e = assertThrows(EntityAlreadyExistsException.class,
                () -> service.createUser(user, Locale.ENGLISH).block());
        assertEquals("User with email \"" + user.getEmail() + "\" is already registered", e.getMessage());
    }

    @Test
    void shouldUpdateFullNameOnUserCreateWhenUserWithUnconfirmedEmailAlreadyExists() {
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
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
        User user = User.builder().email("alice@mail.com").password("secret").fullName("Alice").build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Mono.just(user));

        User newUser = new User(user);
        newUser.setPassword("qwerty");

        service.createUser(newUser, Locale.ENGLISH).block();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(newUser.getPassword(), userCaptor.getValue().getPassword());
    }

    @Test
    void shouldUpdateUser() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        User updatedUser = new User(user);
        updatedUser.setFullName("Alice Wonderland");
        updatedUser.setEnabled(false);

        User expectedResult = new User(updatedUser);
        expectedResult.setPassword(null);

        User result = service.updateUser(updatedUser).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeEmailFieldOnUserUpdate() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        User updatedUser = new User(user);
        updatedUser.setEmail("bob@mail.com");

        User expectedResult = new User(user);
        expectedResult.setPassword(null);

        User result = service.updateUser(updatedUser).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeEmailConfirmedFieldOnUserUpdate() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        User updatedUser = new User(user);
        updatedUser.setEmailConfirmed(false);

        User expectedResult = new User(user);
        expectedResult.setPassword(null);

        User result = service.updateUser(updatedUser).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeAdminFieldOnUserUpdate() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        User updatedUser = new User(user);
        updatedUser.setAdmin(true);

        User expectedResult = new User(user);
        expectedResult.setPassword(null);

        User result = service.updateUser(updatedUser).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeAuthoritiesFieldOnUserUpdate() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        User updatedUser = new User(user);
        updatedUser.setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_SUPER_HERO")));

        User expectedResult = new User(user);
        expectedResult.setPassword(null);

        User result = service.updateUser(updatedUser).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldNotAllowToChangeCreatedAtFieldOnUserUpdate() {
        User user = User.builder()
                .id(1L)
                .email("alice@mail.com")
                .emailConfirmed(true)
                .enabled(true)
                .password("secret")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC).minus(1, ChronoUnit.DAYS))
                .build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(args -> Mono.just(args.getArgument(0)));

        User updatedUser = new User(user);
        updatedUser.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));

        User result = service.updateUser(updatedUser).block();
        assertNotNull(result);
        assertEquals(user.getCreatedAt(), result.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionOnUserUpdateWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.updateUser(null));
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnUserUpdateWhenUserIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").emailConfirmed(true).enabled(true).build();
        when(userRepository.findById(user.getId())).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.updateUser(user).block());
        assertEquals("User with id " + user.getId() + " is not found", e.getMessage());
    }

    @Test
    void shouldReturnProfilePicture() {
        Long userId = 1L;
        ProfilePicture profilePicture = ProfilePicture.builder().userId(userId).build();
        when(profilePictureRepository.findById(userId)).thenReturn(Mono.just(profilePicture));

        ProfilePicture result = service.getProfilePicture(profilePicture.getUserId()).block();
        assertEquals(profilePicture, result);
    }

    @Test
    void shouldThrowExceptionOnProfilePictureGetWhenProfilePictureIsNotFound() {
        Long userId = 1L;
        when(profilePictureRepository.findById(userId)).thenReturn(Mono.empty());
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.getProfilePicture(userId).block());
        assertEquals("Profile picture associated with user with id " + userId + " is not found", e.getMessage());
    }

    @Test
    void shouldCreateProfilePicture() {
        long userId = 1L;
        when(profilePictureRepository.findById(userId)).thenReturn(Mono.empty());
        when(profilePictureRepository.create(any(ProfilePicture.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        ProfilePicture profilePicture = ProfilePicture.builder().userId(userId).data(new byte[] {0}).build();

        ProfilePicture result = service.saveProfilePicture(profilePicture).block();
        assertNotNull(result);
        verify(profilePictureRepository, times(1)).create(profilePicture);
    }

    @Test
    void shouldUpdateProfilePicture() {
        long userId = 1L;
        ProfilePicture profilePicture = ProfilePicture.builder().userId(userId).data(new byte[] {0}).build();
        when(profilePictureRepository.findById(userId)).thenReturn(Mono.just(profilePicture));
        when(profilePictureRepository.save(any(ProfilePicture.class)))
                .thenAnswer(args -> Mono.just(args.getArgument(0)));

        ProfilePicture result = service.saveProfilePicture(profilePicture).block();
        assertNotNull(result);
        verify(profilePictureRepository, times(1)).save(profilePicture);
    }

    @Test
    void shouldThrowExceptionOnProfilePictureSaveWhenProfilePictureIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.saveProfilePicture(null));
        assertEquals("Profile picture must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnProfilePictureSaveWhenProfilePictureFileIsTooLarge() {
        service.setProfilePictureFileMaxSize(1);
        ProfilePicture profilePicture = ProfilePicture.builder().data(new byte[] {0, 1}).build();
        FileTooLargeException e = assertThrows(FileTooLargeException.class,
                () -> service.saveProfilePicture(profilePicture).block());
        assertEquals("Profile picture file size must not be greater than 1 byte(s)", e.getMessage());
    }
}
