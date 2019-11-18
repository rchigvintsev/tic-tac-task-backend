package org.briarheart.orchestra.security.web.server.authentication.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Setter;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.InvalidAccessTokenException;
import org.springframework.util.Assert;

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

    private final SecretKey accessTokenSigningKey;

    @Setter
    private long accessTokenValiditySeconds = DEFAULT_ACCESS_TOKEN_VALIDITY_SECONDS;

    /**
     * Creates new instance of this class with the given token signing key.
     *
     * @param accessTokenSigningKey BASE64-encoded JWT signing key (must not be {@code null} or empty)
     */
    public JwtService(String accessTokenSigningKey) {
        Assert.hasText(accessTokenSigningKey, "Access token signing key must not be null or empty");
        this.accessTokenSigningKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(accessTokenSigningKey));
    }

    @Override
    public Jwt createAccessToken(User user) {
        Assert.notNull(user, "User must not be null");

        Claims claims = Jwts.claims();
        claims.setSubject(user.getEmail());
        claims.put(JwtClaim.FULL_NAME.getName(), user.getFullName());
        claims.put(JwtClaim.PROFILE_PICTURE_URL.getName(), user.getImageUrl());

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime expiration = now.plus(accessTokenValiditySeconds, ChronoUnit.SECONDS);

        claims.setIssuedAt(Date.from(now.toInstant(ZoneOffset.UTC)));
        claims.setExpiration(Date.from(expiration.toInstant(ZoneOffset.UTC)));

        String tokenValue = Jwts.builder()
                .setClaims(claims)
                .signWith(accessTokenSigningKey, SignatureAlgorithm.HS512)
                .compact();
        return new Jwt.Builder(tokenValue).claims(claims).build();
    }

    @Override
    public Jwt parseAccessToken(String tokenValue) throws InvalidAccessTokenException {
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
