package org.briarheart.orchestra.service;

import org.briarheart.orchestra.model.User;
import reactor.core.publisher.Mono;

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
     * @return created user
     */
    Mono<User> createUser(User user);
}
