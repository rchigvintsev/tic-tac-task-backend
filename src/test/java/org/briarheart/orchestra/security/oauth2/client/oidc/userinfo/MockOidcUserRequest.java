package org.briarheart.orchestra.security.oauth2.client.oidc.userinfo;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * Extension of {@link OidcUserRequest} useful for testing purposes.
 *
 * @author Roman Chigvintsev
 */
public class MockOidcUserRequest extends OidcUserRequest {
    /**
     * Creates new instance of this class with the given client registration and claims.
     *
     * @param clientRegistration client registration (must not be {@code null})
     * @param claims claims (must not be {@code null})
     */
    public MockOidcUserRequest(ClientRegistration clientRegistration, Map<String, Object> claims) {
        super(clientRegistration, new MockOAuth2AccessToken(clientRegistration.getScopes()),
                new MockOidcIdToken(claims));
    }

    private static class MockOAuth2AccessToken extends OAuth2AccessToken {
        private static final String TOKEN_VALUE = "Qk45NG5PY2V0TERYOHRoeXlDNzQ=";

        public MockOAuth2AccessToken(Set<String> scopes) {
            super(TokenType.BEARER, TOKEN_VALUE, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), scopes);
        }
    }

    private static class MockOidcIdToken extends OidcIdToken {
        private static final String TOKEN_VALUE = "TFJ6d01LSmhvOGFhbHZJeHFWbDY=";

        public MockOidcIdToken(Map<String, Object> claims) {
            super(TOKEN_VALUE, Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS), claims);
        }
    }
}
