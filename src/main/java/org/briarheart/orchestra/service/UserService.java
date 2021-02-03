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
     * Creates new user.
     *
     * @param user user to be created (must not be {@code null})
     * @param @param locale current user locale
     * @return created user
     * @throws EntityAlreadyExistsException if user with the given email already exists
     */
    Mono<User> createUser(User user, Locale locale) throws EntityAlreadyExistsException;
}
