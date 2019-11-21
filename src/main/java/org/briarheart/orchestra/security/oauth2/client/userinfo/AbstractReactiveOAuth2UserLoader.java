package org.briarheart.orchestra.security.oauth2.client.userinfo;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

/**
 * Base class for implementations of {@link ReactiveOAuth2UserLoader}. By default it delegates user loading to
 * the given {@link ReactiveOAuth2UserService}.
 *
 * @author Roman Chigvintsev
 */
@RequiredArgsConstructor
public abstract class AbstractReactiveOAuth2UserLoader<R extends OAuth2UserRequest, U extends OAuth2User>
        implements ReactiveOAuth2UserLoader<R, U> {
    private final ReactiveOAuth2UserService<R, U> userService;

    @Override
    public Mono<U> loadUser(R userRequest) throws OAuth2AuthenticationException {
        return userService.loadUser(userRequest);
    }
}
