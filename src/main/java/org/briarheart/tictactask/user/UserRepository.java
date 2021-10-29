package org.briarheart.tictactask.user;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    @Query("SELECT * FROM users ORDER BY id ASC LIMIT :limit OFFSET :offset")
    Flux<User> findAllOrderByIdAsc(long offset, Integer limit);

    @Query("SELECT * FROM users WHERE email = :email")
    Mono<User> findByEmail(String email);

    @Query("SELECT * FROM users WHERE email = :email AND enabled = :enabled")
    Mono<User> findByEmailAndEnabled(String email, boolean enabled);

    @Query("SELECT * FROM users WHERE id = :id AND email = :email")
    Mono<User> findByIdAndEmail(Long id, String email);
}
