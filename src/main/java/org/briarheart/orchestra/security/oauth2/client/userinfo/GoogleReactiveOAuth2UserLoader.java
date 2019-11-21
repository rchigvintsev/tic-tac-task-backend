package org.briarheart.orchestra.security.oauth2.client.userinfo;

import org.briarheart.orchestra.security.oauth2.core.user.GoogleOAuth2User;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Google specific OAuth 2 user loader.
 *
 * @author Roman Chigvintsev
 */
public class GoogleReactiveOAuth2UserLoader extends AbstractReactiveOAuth2UserLoader<OidcUserRequest, OidcUser> {
    /**
     * Creates new instance of this class with the given implementation of {@link ReactiveOAuth2UserService}.
     *
     * @param userService OAuth 2 user service (must not be {@code null})
     */
    public GoogleReactiveOAuth2UserLoader(ReactiveOAuth2UserService<OidcUserRequest, OidcUser> userService) {
        super(userService);
    }

    @Override
    public boolean supports(ClientRegistration clientRegistration) {
        return "google".equals(clientRegistration.getRegistrationId());
    }

    @Override
    public Mono<OidcUser> loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest).map(user -> {
            ClientRegistration clientRegistration = userRequest.getClientRegistration();
            ProviderDetails providerDetails = clientRegistration.getProviderDetails();
            String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
            if (!StringUtils.hasText(userNameAttributeName)) {
                userNameAttributeName = IdTokenClaimNames.SUB;
            }
            return new GoogleOAuth2User(user.getAuthorities(), user.getIdToken(), user.getUserInfo(),
                    userNameAttributeName);
        });
    }
}
