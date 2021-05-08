package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link OAuth2AuthorizationResponseConverter} for the authorization code grant type.
 *
 * @author Roman Chigvintsev
 */
public class DefaultOAuth2AuthorizationResponseConverter implements OAuth2AuthorizationResponseConverter {
    @Override
    public OAuth2AuthorizationResponse convert(MultiValueMap<String, String> request, String redirectUri) {
        Assert.notNull(request, "Request parameters must not be null");

        String code = getAuthorizationCode(request);
        String errorCode = getErrorCode(request);
        String state = getState(request);

        if (StringUtils.hasText(code)) {
            return OAuth2AuthorizationResponse.success(code).redirectUri(redirectUri).state(state).build();
        }

        String errorDescription = getErrorDescription(request);
        String errorUri = request.getFirst(OAuth2ParameterNames.ERROR_URI);
        return OAuth2AuthorizationResponse.error(errorCode)
                .redirectUri(redirectUri)
                .errorDescription(errorDescription)
                .errorUri(errorUri)
                .state(state)
                .build();
    }

    protected String getAuthorizationCode(MultiValueMap<String, String> request) {
        return request.getFirst(OAuth2ParameterNames.CODE);
    }

    protected String getErrorCode(MultiValueMap<String, String> request) {
        return request.getFirst(OAuth2ParameterNames.ERROR);
    }

    protected String getErrorDescription(MultiValueMap<String, String> request) {
        return request.getFirst(OAuth2ParameterNames.ERROR_DESCRIPTION);
    }

    protected String getState(MultiValueMap<String, String> request) {
        return request.getFirst(OAuth2ParameterNames.STATE);
    }
}
