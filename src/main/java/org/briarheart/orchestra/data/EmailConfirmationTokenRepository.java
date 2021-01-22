package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Roman Chigvintsev
 */
public interface EmailConfirmationTokenRepository extends ReactiveCrudRepository<EmailConfirmationToken, Long> {
}
