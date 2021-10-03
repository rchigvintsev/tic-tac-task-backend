package org.briarheart.tictactask.security.oauth2.client.userinfo;

import org.briarheart.tictactask.security.oauth2.core.user.FacebookOAuth2User;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Facebook specific OAuth 2 user loader.
 *
 * @author Roman Chigvintsev
 */
public class FacebookReactiveOAuth2UserLoader extends AbstractReactiveOAuth2UserLoader<OAuth2UserRequest, OAuth2User> {
    private static final String PICTURE_URI_TEMPLATE = "http://graph.facebook.com/{user-id}/picture?type=normal";

    /**
     * Creates new instance of this class with the given implementation of {@link ReactiveOAuth2UserService}.
     *
     * @param userService OAuth 2 user service (must not be {@code null})
     */
    public FacebookReactiveOAuth2UserLoader(ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
        super(userService);
    }

    @Override
    public boolean supports(ClientRegistration clientRegistration) {
        return "facebook".equals(clientRegistration.getRegistrationId());
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest).map(user -> {
            Map<String, Object> userAttrs = user.getAttributes();
            if (userAttrs.get("picture") == null) {
                String pictureUri = UriComponentsBuilder.fromUriString(PICTURE_URI_TEMPLATE)
                        .build(user.getName())
                        .toString();
                userAttrs = new HashMap<>(userAttrs);
                userAttrs.put("picture", pictureUri);
            }

            ClientRegistration clientRegistration = userRequest.getClientRegistration();
            ProviderDetails providerDetails = clientRegistration.getProviderDetails();
            String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
            return new FacebookOAuth2User(user.getAuthorities(), userAttrs, userNameAttributeName);
        });
    }
}
