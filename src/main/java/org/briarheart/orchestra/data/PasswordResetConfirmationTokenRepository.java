package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.PasswordResetConfirmationToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface PasswordResetConfirmationTokenRepository extends ReactiveCrudRepository<PasswordResetConfirmationToken, Long> {
    // Limit records in case (although unlikely) token values are duplicated
    @Query("SELECT * FROM password_reset_token WHERE user_id = :userId AND token_value = :tokenValue"
            + " ORDER BY created_at DESC LIMIT 1")
    Mono<PasswordResetConfirmationToken> findFirstByUserIdAndTokenValueOrderByCreatedAtDesc(
            Long userId,
            String tokenValue
    );
}
