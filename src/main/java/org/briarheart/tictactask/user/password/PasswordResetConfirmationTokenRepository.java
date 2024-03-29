package org.briarheart.tictactask.user.password;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface PasswordResetConfirmationTokenRepository
        extends ReactiveCrudRepository<PasswordResetConfirmationToken, Long> {
    // Limit records in case (although unlikely) token values are duplicated
    @Query("SELECT * FROM password_reset_confirmation_token WHERE user_id = :userId AND token_value = :tokenValue"
            + " AND valid = :valid"
            + " ORDER BY created_at DESC LIMIT 1")
    Mono<PasswordResetConfirmationToken> findFirstByUserIdAndTokenValueAndValidOrderByCreatedAtDesc(
            Long userId,
            String tokenValue,
            boolean valid
    );
}
