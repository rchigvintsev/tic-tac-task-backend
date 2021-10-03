package org.briarheart.tictactask.service;

import org.briarheart.tictactask.data.EntityAlreadyExistsException;
import org.briarheart.tictactask.data.EntityNotFoundException;
import org.briarheart.tictactask.model.ProfilePicture;
import org.briarheart.tictactask.model.User;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Service for user managing.
 *
 * @author Roman Chigvintsev
 * @see User
 */
public interface UserService {
    /**
     * Returns number of all users.
     *
     * @return number of all registered users
     */
    Mono<Long> getUserCount();

    /**
     * Returns all users.
     *
     * @param pageable paging restriction
     * @return registered users
     */
    Flux<User> getUsers(Pageable pageable);

    /**
     * Creates new user along with sending email confirmation link to user's email address. Newly created user will be
     * disabled until his/her email is confirmed.
     *
     * @param user   user to be created (must not be {@code null})
     * @param locale current user's locale
     * @return created user
     * @throws EntityAlreadyExistsException if user with the given email already exists
     */
    Mono<User> createUser(User user, Locale locale) throws EntityAlreadyExistsException;

    /**
     * Updates user.
     *
     * @param user user to be updated (must not be {@code null})
     * @return updated user
     * @throws EntityNotFoundException if user is not found
     */
    Mono<User> updateUser(User user) throws EntityNotFoundException;

    /**
     * Returns profile picture associated with user with the given id.
     *
     * @param userId user id
     * @return profile picture or empty stream if user does not have a profile picture
     * @throws EntityNotFoundException if user is not found by id
     */
    Mono<ProfilePicture> getProfilePicture(Long userId) throws EntityNotFoundException;

    /**
     * Creates new or updates existing profile picture.
     *
     * @param picture profile picture to be created/updated (must not be {@code null})
     * @return created/updated profile picture
     */
    Mono<ProfilePicture> saveProfilePicture(ProfilePicture picture);
}
