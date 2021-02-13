package org.briarheart.orchestra.security.oauth2.core.userdetails;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.UserAuthorityRelationRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.UserAuthorityRelation;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Implementation of {@link ReactiveUserDetailsService} that load users from database.
 *
 * @author Roman Chigvintsev
 */
@RequiredArgsConstructor
public class DatabaseReactiveUserDetailsService implements ReactiveUserDetailsService {
    private final UserRepository userRepository;
    private final UserAuthorityRelationRepository userAuthorityRelationRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByEmail(username)
                .flatMap(user -> loadAuthorities(user.getId()).map(authorities -> {
                    user.setAuthorities(authorities);
                    return user;
                }))
                .cast(UserDetails.class);
    }

    private Mono<List<SimpleGrantedAuthority>> loadAuthorities(Long userId) {
        Flux<UserAuthorityRelation> userAuthorityRelations = userAuthorityRelationRepository.findByUserId(userId);
        return userAuthorityRelations.map(relations -> new SimpleGrantedAuthority(relations.getAuthority()))
                .collectList();
    }
}
