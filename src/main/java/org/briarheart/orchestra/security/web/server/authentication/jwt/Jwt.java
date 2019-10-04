package org.briarheart.orchestra.security.web.server.authentication.jwt;

import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.security.web.server.authentication.AccessToken;
import org.springframework.util.Assert;

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

    public static class Builder {
        private final String tokenValue;
        private final Map<String, Object> claims = new HashMap<>();

        public Builder(String tokenValue) {
            Assert.hasText(tokenValue, "Token value must not be null or empty");
            this.tokenValue = tokenValue;
        }

        public Builder claims(Map<String, Object> claims) {
            if (claims != null && !claims.isEmpty())
                this.claims.putAll(claims);
            return this;
        }

        public Jwt build() {
            Jwt jwt = new Jwt(tokenValue);
            jwt.claims.putAll(claims);
            return jwt;
        }
    }
}
