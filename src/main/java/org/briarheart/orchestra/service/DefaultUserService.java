package org.briarheart.orchestra.service;

import io.jsonwebtoken.lang.Assert;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.ProfilePictureRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.ProfilePicture;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.util.Pageables;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Default implementation of {@link UserService}.
 *
 * @author Roman Chigvintsev
 */
@Service
@Slf4j
public class DefaultUserService implements UserService {
    private static final int DEFAULT_PROFILE_PICTURE_FILE_MAX_SIZE = 1024 * 1024 * 3; // 3 Mb

    private final UserRepository userRepository;
    private final ProfilePictureRepository profilePictureRepository;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordEncoder passwordEncoder;
    private final MessageSourceAccessor messages;

    @Setter
    private int profilePictureFileMaxSize = DEFAULT_PROFILE_PICTURE_FILE_MAX_SIZE;

    public DefaultUserService(UserRepository userRepository,
                              ProfilePictureRepository profilePictureRepository,
                              EmailConfirmationService emailConfirmationService,
                              PasswordEncoder passwordEncoder,
                              MessageSourceAccessor messages) {
        Assert.notNull(userRepository, "User repository must not be null");
        Assert.notNull(profilePictureRepository, "Profile picture repository must not be null");
        Assert.notNull(emailConfirmationService, "Email confirmation service must not be null");
        Assert.notNull(passwordEncoder, "Password encoder must not be null");
        Assert.notNull(messages, "Message source accessor must not be null");

        this.userRepository = userRepository;
        this.profilePictureRepository = profilePictureRepository;
        this.emailConfirmationService = emailConfirmationService;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
    }

    @Override
    public Mono<Long> getUserCount() {
        return userRepository.count();
    }

    @Override
    public Flux<User> getUsers(Pageable pageable) {
        return userRepository.findAllOrderByIdAsc(Pageables.getOffset(pageable), Pageables.getLimit(pageable))
                .map(this::clearPassword);
    }

    @Transactional
    @Override
    public Mono<User> createUser(User user, Locale locale) throws EntityAlreadyExistsException {
        Assert.notNull(user, "User must not be null");

        String email = user.getEmail();
        return userRepository.findByEmail(email)
                .flatMap(u -> ensureEmailNotConfirmed(u, locale))
                .flatMap(u -> {
                    u.setFullName(user.getFullName());
                    u.setPassword(encodePassword(user.getPassword()));
                    return userRepository.save(u);
                })
                .switchIfEmpty(createNewUser(user)
                        .doOnSuccess(u -> log.debug("User with id {} is created", u.getId())))
                .map(this::clearPassword)
                .flatMap(u -> emailConfirmationService.sendEmailConfirmationLink(u, locale).map(token -> u));
    }

    @Transactional
    @Override
    public Mono<User> updateUser(User user) {
        Assert.notNull(user, "User must not be null");
        Long userId = user.getId();
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("User with id " + userId + " is not found")))
                .flatMap(existingUser -> {
                    if (user.getEnabled() != null) {
                        existingUser.setEnabled(user.getEnabled());
                    }
                    existingUser.setFullName(user.getFullName());
                    existingUser.setProfilePictureUrl(user.getProfilePictureUrl());
                    return userRepository.save(existingUser)
                            .map(this::clearPassword)
                            .doOnSuccess(u -> log.debug("User with id {} is updated", u.getId()));
                });
    }

    @Override
    public Mono<ProfilePicture> getProfilePicture(Long userId) throws EntityNotFoundException {
        return profilePictureRepository.findById(userId)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Profile picture associated with user with id "
                        + userId + " is not found")));
    }

    @Transactional
    @Override
    public Mono<ProfilePicture> saveProfilePicture(ProfilePicture picture) {
        Assert.notNull(picture, "Profile picture must not be null");
        if (picture.getData().length > profilePictureFileMaxSize) {
            return Mono.error(new FileTooLargeException("Profile picture file size must not be greater than "
                    + profilePictureFileMaxSize + " byte(s)"));
        }

        return profilePictureRepository.findById(picture.getUserId())
                .hasElement()
                .flatMap(found -> {
                    if (found) {
                        return profilePictureRepository.save(picture);
                    }
                    return profilePictureRepository.create(picture);
                });
    }

    private Mono<User> ensureEmailNotConfirmed(User user, Locale locale) {
        if (user.isEmailConfirmed()) {
            String message = messages.getMessage("user.registration.user-already-registered",
                    new Object[]{user.getEmail()}, Locale.ENGLISH);
            String localizedMessage;
            if (locale == null || locale == Locale.ENGLISH) {
                localizedMessage = message;
            } else {
                localizedMessage = messages.getMessage("user.registration.user-already-registered",
                        new Object[]{user.getEmail()}, locale);
            }
            return Mono.error(new EntityAlreadyExistsException(message, localizedMessage));
        }
        return Mono.just(user);
    }

    private Mono<User> createNewUser(User user) {
        return Mono.defer(() -> {
            User newUser = new User();
            newUser.setEmail(user.getEmail());
            newUser.setPassword(encodePassword(user.getPassword()));
            newUser.setFullName(user.getFullName());
            newUser.setProfilePictureUrl(user.getProfilePictureUrl());
            return userRepository.save(newUser);
        });
    }

    private String encodePassword(String rawPassword) {
        return rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
    }

    private User clearPassword(User user) {
        user.setPassword(null);
        return user;
    }
}
