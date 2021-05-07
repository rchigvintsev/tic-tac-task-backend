package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link OAuth2AuthorizationResponseConverter} for Authorization Code Flow.
 *
 * @author Roman Chigvintsev
 */
public class DefaultOAuth2AuthorizationResponseConverter implements OAuth2AuthorizationResponseConverter {
    @Override
    public OAuth2AuthorizationResponse convert(MultiValueMap<String, String> request, String redirectUri) {
        Assert.notNull(request, "Request parameters must not be null");

        String code = request.getFirst(OAuth2ParameterNames.CODE);
        String errorCode = request.getFirst(OAuth2ParameterNames.ERROR);
        String state = request.getFirst(OAuth2ParameterNames.STATE);

        if (StringUtils.hasText(code)) {
            return OAuth2AuthorizationResponse.success(code).redirectUri(redirectUri).state(state).build();
        }

        String errorDescription = request.getFirst(OAuth2ParameterNames.ERROR_DESCRIPTION);
        String errorUri = request.getFirst(OAuth2ParameterNames.ERROR_URI);
        return OAuth2AuthorizationResponse.error(errorCode)
                .redirectUri(redirectUri)
                .errorDescription(errorDescription)
                .errorUri(errorUri)
                .state(state)
                .build();
    }
}
