package org.briarheart.tictactask.security.web.server.authentication.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Setter;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.InvalidAccessTokenException;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.briarheart.tictactask.user.User;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * JWT based implementation of {@link AccessTokenService}. By default this service issues signed JWTs with expiration
 * timeout of ten minutes.
 *
 * @author Roman Chigvintsev
 *
 * @see Jwt
 */
public class JwtService implements AccessTokenService {
    private static final long DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS = 300;

    private final ServerAccessTokenRepository accessTokenRepository;
    private final SecretKey accessTokenSigningKey;

    @Setter
    private long accessTokenValiditySeconds = DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS;

    /**
     * Creates new instance of this class with the given access token repository and signing key.
     *
     * @param accessTokenRepository access token repository (must not be {@code null})
     * @param accessTokenSigningKey BASE64-encoded JWT signing key (must not be {@code null} or empty)
     */
    public JwtService(ServerAccessTokenRepository accessTokenRepository, String accessTokenSigningKey) {
        Assert.notNull(accessTokenRepository, "Access token repository must not be null");
        Assert.hasText(accessTokenSigningKey, "Access token signing key must not be null or empty");

        this.accessTokenRepository = accessTokenRepository;
        this.accessTokenSigningKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(accessTokenSigningKey));
    }

    @Override
    public Mono<? extends AccessToken> createAccessToken(User user, ServerWebExchange exchange) {
        Assert.notNull(user, "User must not be null");
        Assert.notNull(exchange, "Server web exchange must not be null");

        return Mono.defer(() -> {
            Claims claims = Jwts.claims();
            claims.setSubject(user.getId().toString());
            claims.put(JwtClaim.EMAIL.getName(), user.getEmail());
            claims.put(JwtClaim.FULL_NAME.getName(), user.getFullName());
            claims.put(JwtClaim.PROFILE_PICTURE_URL.getName(), user.getProfilePictureUrl());
            claims.put(JwtClaim.ADMIN.getName(), user.isAdmin());

            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime expiration = now.plus(accessTokenValiditySeconds, ChronoUnit.SECONDS);

            claims.setIssuedAt(Date.from(now.toInstant(ZoneOffset.UTC)));
            claims.setExpiration(Date.from(expiration.toInstant(ZoneOffset.UTC)));

            String tokenValue = Jwts.builder()
                    .setClaims(claims)
                    .signWith(accessTokenSigningKey, SignatureAlgorithm.HS512)
                    .compact();
            Jwt token = new Jwt.Builder(tokenValue).claims(claims).build();
            return accessTokenRepository.saveAccessToken(token, exchange);
        });
    }

    @Override
    public Mono<? extends AccessToken> parseAccessToken(String tokenValue) {
        Assert.hasText(tokenValue, "Access token value must not be null or empty");
        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(accessTokenSigningKey)
                        .parseClaimsJws(tokenValue)
                        .getBody();
                return new Jwt.Builder(tokenValue).claims(claims).build();
            } catch (ExpiredJwtException e) {
                throw new InvalidAccessTokenException("Access token is expired", e);
            } catch (UnsupportedJwtException e) {
                throw new InvalidAccessTokenException("Access token is unsupported", e);
            } catch (MalformedJwtException e) {
                throw new InvalidAccessTokenException("Access token is malformed", e);
            } catch (SignatureException e) {
                throw new InvalidAccessTokenException("Access token signature is not valid", e);
            }
        });
    }
}
