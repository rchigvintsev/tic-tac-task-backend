package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Image;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface ImageRepository extends ReactiveCrudRepository<Image, Long> {
    @Query("SELECT * FROM image WHERE id = :id AND user_id = :userId")
    Mono<Image> findByIdAndUserId(Long id, Long userId);
}
