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
    Mono<User> createUser(User user);
}
