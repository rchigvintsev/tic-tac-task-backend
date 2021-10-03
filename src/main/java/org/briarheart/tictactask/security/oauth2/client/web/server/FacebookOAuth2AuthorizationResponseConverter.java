package org.briarheart.tictactask.security.oauth2.client.web.server;

import org.springframework.util.MultiValueMap;

/**
 * Implementation of {@link OAuth2AuthorizationResponseConverter} for Facebook authorization server. The only difference
 * of this converter from {@link DefaultOAuth2AuthorizationResponseConverter} is in way how it obtains error code
 * from request parameters (using key "error_code" instead of "error").
 *
 * @author Roman Chigvintsev
 */
public class FacebookOAuth2AuthorizationResponseConverter extends DefaultOAuth2AuthorizationResponseConverter {
    @Override
    protected String getErrorCode(MultiValueMap<String, String> request) {
        return request.getFirst("error_code");
    }

    @Override
    protected String getErrorDescription(MultiValueMap<String, String> request) {
        return request.getFirst("error_message");
    }
}
