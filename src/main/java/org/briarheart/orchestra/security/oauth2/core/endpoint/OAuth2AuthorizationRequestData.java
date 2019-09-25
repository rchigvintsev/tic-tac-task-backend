package org.briarheart.orchestra.security.oauth2.core.endpoint;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is a helper class to build JSON representation of {@link OAuth2AuthorizationRequest}.
 *
 * @author Roman Chigvintsev
 *
 * @see OAuth2AuthorizationRequest
 */
@Data
@NoArgsConstructor
public class OAuth2AuthorizationRequestData {
    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthorizationRequestData.class);

    private String authorizationUri;
    private String grantType;
    private String clientId;
    private String redirectUri;
    private Set<String> scopes;
    private String state;
    private Map<String, Object> additionalParameters;
    private String authorizationRequestUri;
    private Map<String, Object> attributes;

    /**
     * Creates new instance of this class from {@link OAuth2AuthorizationRequest}.
     *
     * @param request authorization request that need to be represented in JSON (must not be {@code null})
     */
    public OAuth2AuthorizationRequestData(OAuth2AuthorizationRequest request) {
        Assert.notNull(request, "Authorization request must not be null");

        this.authorizationUri = request.getAuthorizationUri();
        this.grantType = request.getGrantType().getValue();
        this.clientId = request.getClientId();
        this.redirectUri = request.getRedirectUri();
        this.scopes = request.getScopes();
        this.state = request.getState();
        this.additionalParameters = new HashMap<>(request.getAdditionalParameters());
        this.authorizationRequestUri = request.getAuthorizationRequestUri();
        this.attributes = new HashMap<>(request.getAttributes());
    }

    /**
     * Converts instance of this class to {@link OAuth2AuthorizationRequest}.
     *
     * @return new instance of {@link OAuth2AuthorizationRequest}
     *
     * @throws OAuth2AuthenticationException if current grant type is not supported
     */
    public OAuth2AuthorizationRequest toAuthorizationRequest() {
        OAuth2AuthorizationRequest.Builder requestBuilder;
        if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(grantType))
            requestBuilder = OAuth2AuthorizationRequest.authorizationCode();
        else if (AuthorizationGrantType.IMPLICIT.getValue().equals(grantType))
            requestBuilder = OAuth2AuthorizationRequest.implicit();
        else {
            OAuth2Error oAuth2Error = new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT,
                    "Unsupported grant type: " + grantType, null);
            throw new OAuth2AuthenticationException(oAuth2Error);
        }
        return requestBuilder
                .authorizationUri(authorizationUri)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scopes(scopes)
                .state(state)
                .additionalParameters(additionalParameters)
                .authorizationRequestUri(authorizationRequestUri)
                .attributes(attributes)
                .build();
    }
}
