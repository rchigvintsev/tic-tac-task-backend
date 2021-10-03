package org.briarheart.tictactask.security.web.server.authentication.logout;

import org.briarheart.tictactask.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.WebFilterExchange;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Roman Chigvintsev
 */
class AccessTokenLogoutHandlerTest {
    private AccessTokenLogoutHandler handler;
    private ServerAccessTokenRepository accessTokenRepositoryMock;

    @BeforeEach
    void setUp() {
        accessTokenRepositoryMock = mock(ServerAccessTokenRepository.class);
        when(accessTokenRepositoryMock.removeAccessToken(any())).thenReturn(Mono.empty());
        handler = new AccessTokenLogoutHandler(accessTokenRepositoryMock);
    }

    @SuppressWarnings("UnassignedFluxMonoInstance")
    @Test
    void shouldRemoveAccessTokenFromServerWebExchange() {
        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);
        WebFilterExchange webFilterExchangeMock = mock(WebFilterExchange.class);
        when(webFilterExchangeMock.getExchange()).thenReturn(webExchangeMock);

        handler.logout(webFilterExchangeMock, null).block();
        verify(accessTokenRepositoryMock).removeAccessToken(webExchangeMock);
        verifyNoMoreInteractions(accessTokenRepositoryMock);
    }
}
