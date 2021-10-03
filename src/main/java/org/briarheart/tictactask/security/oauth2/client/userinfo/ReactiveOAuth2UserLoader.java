package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

/**
 * Loader responsible for obtaining attributes of the End-User (Resource Owner) from the UserInfo Endpoint using
 * {@link OAuth2UserRequest#getAccessToken() Access Token} granted to
 * {@link OAuth2UserRequest#getClientRegistration() Client} and returning
 * an {@link org.springframework.security.core.AuthenticatedPrincipal} in the form of an {@link OAuth2User}.
 *
 * @author Roman Chigvintsev
 */
public interface ReactiveOAuth2UserLoader<R extends OAuth2UserRequest, U extends OAuth2User> {
    /**
     * Checks if this loader supports the given client registration.
     *
     * @param clientRegistration client registration (must not be {@code null})
     * @return {@code true} if this loader supports the given client registration; {@code false} otherwise
     */
    boolean supports(ClientRegistration clientRegistration);

    /**
     * Loads attributes of the End-User from the UserInfo Endpoint.
     *
     * @param userRequest the user request (must not be {@code null})
     * @return an instance of {@link OAuth2User}
     * @throws OAuth2AuthenticationException if an error occurs while attempting to obtain user attributes from
     *                                       the UserInfo Endpoint
     */
    Mono<U> loadUser(R userRequest) throws OAuth2AuthenticationException;
}
