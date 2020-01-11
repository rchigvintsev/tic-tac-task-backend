package org.briarheart.orchestra.security.web.server.authentication.jwt;

import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessToken;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT based implementation of {@link AccessToken}.
 *
 * @author Roman Chigvintsev
 */
@Data
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Jwt implements AccessToken {
    private static final long serialVersionUID = 1L;

    private final String tokenValue;
    private final Map<String, Object> claims = new HashMap<>();

    @Override
    public String getSubject() {
        return (String) claims.get(Claims.SUBJECT);
    }

    @Override
    public Instant getIssuedAt() {
        return Instant.ofEpochSecond((Long) claims.get(Claims.ISSUED_AT));
    }

    @Override
    public Instant getExpiration() {
        return Instant.ofEpochSecond((Long) claims.get(Claims.EXPIRATION));
    }

    @Override
    public Map<String, Object> getClaims() {
        return claims;
    }

    public static class Builder {
        private final String tokenValue;
        private final Map<String, Object> claims = new HashMap<>();

        public Builder(String tokenValue) {
            Assert.hasText(tokenValue, "Token value must not be null or empty");
            this.tokenValue = tokenValue;
        }

        public Builder claims(Map<String, Object> claims) {
            this.claims.putAll(claims);
            return this;
        }

        public Builder claim(String key, Object value) {
            this.claims.put(key, value);
            return this;
        }

        public Builder claim(JwtClaim key, Object value) {
            return claim(key.getName(), value);
        }

        public Jwt build() {
            Jwt jwt = new Jwt(tokenValue);
            jwt.claims.putAll(claims);
            return jwt;
        }
    }
}
