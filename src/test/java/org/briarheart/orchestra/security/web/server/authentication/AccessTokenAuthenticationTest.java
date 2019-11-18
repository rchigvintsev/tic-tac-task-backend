package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenAuthenticationTest {
    @Test
    void shouldThrowExceptionWhenTryingToMakeTokenAuthenticated() {
        AccessTokenAuthentication token = new AccessTokenAuthentication("S0JtOU5Jd1FHNlp4TnpoczFOcWI=");
        assertFalse(token.isAuthenticated());
        assertThrows(IllegalArgumentException.class, () -> token.setAuthenticated(true));
    }

    @Test
    void shouldMakeTokenUnauthenticated() {
        AccessToken accessTokenMock = mock(AccessToken.class);
        AccessTokenAuthentication token = new AccessTokenAuthentication(accessTokenMock);
        assertTrue(token.isAuthenticated());
        token.setAuthenticated(false);
        assertFalse(token.isAuthenticated());
    }
}
