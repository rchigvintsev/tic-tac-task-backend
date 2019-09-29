package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Roman Chigvintsev
 */
public interface UserRepository extends ReactiveCrudRepository<User, String> {
}
