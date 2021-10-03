package org.briarheart.tictactask.security.web.server.authentication.logout;

import lombok.RequiredArgsConstructor;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import reactor.core.publisher.Mono;

/**
 * Implementation of {@link ServerLogoutHandler} that removes access token from current
 * {@link org.springframework.web.server.ServerWebExchange} using {@link ServerAccessTokenRepository}.
 *
 * @author Roman Chigvintsev
 *
 * @see ServerAccessTokenRepository
 */
@RequiredArgsConstructor
public class AccessTokenLogoutHandler implements ServerLogoutHandler {
    private final ServerAccessTokenRepository accessTokenRepository;

    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        return accessTokenRepository.removeAccessToken(exchange.getExchange()).then();
    }
}
