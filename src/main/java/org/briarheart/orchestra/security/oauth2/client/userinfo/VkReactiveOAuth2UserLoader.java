package org.briarheart.orchestra.security.oauth2.client.userinfo;

import org.briarheart.orchestra.security.oauth2.core.user.VkOAuth2User;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import reactor.core.publisher.Mono;

/**
 * VK specific OAuth 2 user loader.
 *
 * @author Roman Chigvintsev
 */
public class VkReactiveOAuth2UserLoader extends AbstractReactiveOAuth2UserLoader<OAuth2UserRequest, OAuth2User> {
    /**
     * Creates new instance of this class with the given implementation of {@link ReactiveOAuth2UserService}.
     *
     * @param userService OAuth 2 user service (must not be {@code null})
     */
    public VkReactiveOAuth2UserLoader(ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> userService) {
        super(userService);
    }

    @Override
    public boolean supports(ClientRegistration clientRegistration) {
        return "vk".equals(clientRegistration.getRegistrationId());
    }

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest).map(user -> {
            ClientRegistration clientRegistration = userRequest.getClientRegistration();
            ProviderDetails providerDetails = clientRegistration.getProviderDetails();
            String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
            return new VkOAuth2User(user.getAuthorities(), user.getAttributes(), userNameAttributeName);
        });
    }
}
