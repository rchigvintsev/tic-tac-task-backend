package org.briarheart.tictactask.data;

import org.briarheart.tictactask.model.UserAuthorityRelation;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * @author Roman Chigvintsev
 */
public interface UserAuthorityRelationRepository extends ReactiveCrudRepository<UserAuthorityRelation, Void> {
    @Query("SELECT * FROM authorities WHERE user_id = :userId")
    Flux<UserAuthorityRelation> findByUserId(Long userId);
}
