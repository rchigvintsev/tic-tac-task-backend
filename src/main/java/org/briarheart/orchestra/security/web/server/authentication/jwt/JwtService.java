package org.briarheart.orchestra.security.web.server.authentication.jwt;

import io.jsonwebtoken.*;
import lombok.Setter;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.InvalidAccessTokenException;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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

    private final byte[] accessTokenSigningKey;

    @Setter
    private long accessTokenValiditySeconds = DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS;

    /**
     * Creates new instance of this class with the given token signing key.
     *
     * @param accessTokenSigningKey JWT signing key (must not be {@code null} or empty)
     */
    public JwtService(byte[] accessTokenSigningKey) {
        if (accessTokenSigningKey == null || accessTokenSigningKey.length == 0) {
            throw new IllegalArgumentException("Access token signing key must not be null or empty");
        }
        this.accessTokenSigningKey = accessTokenSigningKey;
    }

    @Override
    public AccessToken createAccessToken(User user) {
        Assert.notNull(user, "User must not be null");

        Claims claims = Jwts.claims();
        claims.setSubject(user.getEmail());

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiration = now.plus(accessTokenValiditySeconds, ChronoUnit.SECONDS);

        claims.setIssuedAt(Date.from(now.toInstant(ZoneOffset.UTC)));
        claims.setExpiration(Date.from(expiration.toInstant(ZoneOffset.UTC)));

        String tokenValue = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, accessTokenSigningKey)
                .compact();
        return new Jwt.Builder(tokenValue).claims(claims).build();
    }

    @Override
    public AccessToken parseAccessToken(String tokenValue) throws InvalidAccessTokenException {
        Assert.hasText(tokenValue, "Access token value must not be null or empty");

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
    }
}
