package org.briarheart.orchestra.security.web.server.authentication;

import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.InvalidAccessTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenReactiveAuthenticationManagerTest {
    private static final String ACCESS_TOKEN_VALUE = "NlYwTEh5V2I2anNjQVB0MUhQbTQ=";

    private AccessTokenService accessTokenServiceMock;
    private AccessTokenReactiveAuthenticationManager manager;

    @BeforeEach
    void setUp() {
        accessTokenServiceMock = mock(AccessTokenService.class);
        manager = new AccessTokenReactiveAuthenticationManager(accessTokenServiceMock);
    }

    @Test
    void shouldAuthenticate() {
        AccessToken accessTokenMock = mock(AccessToken.class);
        doReturn(Mono.just(accessTokenMock)).when(accessTokenServiceMock).parseAccessToken(anyString());

        Authentication authenticationMock = new AccessTokenAuthentication(ACCESS_TOKEN_VALUE);
        Authentication authentication = manager.authenticate(authenticationMock).block();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
    }

    @Test
    void shouldReturnAuthenticationWithAuthenticatedPrincipal() {
        AccessToken accessTokenMock = mock(AccessToken.class);
        doReturn(Mono.just(accessTokenMock)).when(accessTokenServiceMock).parseAccessToken(anyString());

        Authentication authenticationMock = new AccessTokenAuthentication(ACCESS_TOKEN_VALUE);
        Authentication authentication = manager.authenticate(authenticationMock).block();
        assertNotNull(authentication);

        Object principal = authentication.getPrincipal();
        assertNotNull(principal);
        assertTrue(principal instanceof AuthenticatedPrincipal);
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsInvalid() {
        when(accessTokenServiceMock.parseAccessToken(anyString())).thenThrow(InvalidAccessTokenException.class);
        Authentication authenticationMock = new AccessTokenAuthentication(ACCESS_TOKEN_VALUE);
        assertThrows(InvalidAccessTokenException.class, () -> manager.authenticate(authenticationMock).block());
    }
}
