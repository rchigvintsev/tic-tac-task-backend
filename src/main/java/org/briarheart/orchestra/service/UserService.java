package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityAlreadyExistsException;
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
     * Sends password reset link to the given email address provided user with such email address exists and enabled.
     * Does nothing otherwise.
     *
     * @param email email address of user whose password should be reset (must not be {@code null} or empty)
     * @param locale current user's locale
     */
    Mono<Void> resetPassword(String email, Locale locale);
}
