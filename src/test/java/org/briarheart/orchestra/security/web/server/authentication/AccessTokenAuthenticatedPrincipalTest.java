package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenAuthenticatedPrincipalTest {
    @Test
    void shouldUseAccessTokenSubjectAsPrincipalName() {
        AccessToken accessTokenMock = mock(AccessToken.class);
        when(accessTokenMock.getSubject()).thenReturn("alice");
        AccessTokenAuthenticatedPrincipal principal = new AccessTokenAuthenticatedPrincipal(accessTokenMock);
        assertEquals(accessTokenMock.getSubject(), principal.getName());
    }
}
