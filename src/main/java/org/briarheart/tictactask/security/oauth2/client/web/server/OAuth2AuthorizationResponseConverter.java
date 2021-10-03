package org.briarheart.tictactask.security.oauth2.client.web.server;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.util.MultiValueMap;

/**
 * A strategy used for converting authorization callback request to instance of {@link OAuth2AuthorizationResponse}.
 *
 * @author Roman Chigvintsev
 */
public interface OAuth2AuthorizationResponseConverter {
    /**
     * Converts OAuth 2.0 authorization callback request to instance of {@link OAuth2AuthorizationResponse}.
     *
     * @param request request parameters (must not be {@code null})
     * @param redirectUri redirect URI
     * @return instance of {@link OAuth2AuthorizationResponse}
     */
    OAuth2AuthorizationResponse convert(MultiValueMap<String, String> request, String redirectUri);
}
