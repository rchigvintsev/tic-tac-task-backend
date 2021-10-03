package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.briarheart.tictactask.security.oauth2.core.user.GithubOAuth2User;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

/**
 * Github specific OAuth 2 user loader.
 *
 * @author Roman Chigvintsev
 */
public class GithubReactiveOAuth2UserLoader extends AbstractReactiveOAuth2UserLoader<OAuth2UserRequest, OAuth2User> {
    /**
     * Creates new instance of this class with the given implementation of {@link ReactiveOAuth2UserService}.
     *
     * @param userService OAuth 2 user service (must not be {@code null})
     */
    public GithubReactiveOAuth2UserLoader(ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
        super(userService);
    }

    @Override
    public boolean supports(ClientRegistration clientRegistration) {
        return "github".equals(clientRegistration.getRegistrationId());
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest).map(user -> {
            ClientRegistration clientRegistration = userRequest.getClientRegistration();
            ClientRegistration.ProviderDetails providerDetails = clientRegistration.getProviderDetails();
            String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
            return new GithubOAuth2User(user.getAuthorities(), user.getAttributes(), userNameAttributeName);
        });
    }
}
