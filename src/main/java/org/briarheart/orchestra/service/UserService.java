package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.User;
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
     * Creates new user. Newly created user will be disabled until his/her email is confirmed.
     *
     * @param user   user to be created (must not be {@code null})
     * @param locale current user's locale
     * @return created user
     * @throws EntityAlreadyExistsException if user with the given email already exists
     */
    Mono<User> createUser(User user, Locale locale) throws EntityAlreadyExistsException;

    /**
     * Confirms email for user with the given id.
     *
     * @param userId id of user whose email must be confirmed
     * @param token  email confirmation token
     * @throws EntityNotFoundException                if user is not found by id or given email confirmation token is
     *                                                not registered for user
     * @throws EmailConfirmationTokenExpiredException if given email confirmation token is expired
     */
    Mono<Void> confirmEmail(Long userId, String token) throws EntityNotFoundException,
            EmailConfirmationTokenExpiredException;
}
