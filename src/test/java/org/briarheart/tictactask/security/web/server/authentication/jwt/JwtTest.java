package org.briarheart.tictactask.security.web.server.authentication.jwt;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Roman Chigvintsev
 */
class JwtTest {
    @Test
    void shouldReturnIssuedAtClaim() {
        final long IAT = 1570969232L;
        Jwt token = new Jwt.Builder(TestJwts.DEFAULT_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.ISSUED_AT, IAT)
                .build();
        assertEquals(Instant.ofEpochSecond(IAT), token.getIssuedAt());
    }

    @Test
    void shouldReturnExpirationClaim() {
        final long EXP = 9214159449945032L;
        Jwt token = new Jwt.Builder(TestJwts.DEFAULT_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.EXPIRATION_TIME, EXP)
                .build();
        assertEquals(Instant.ofEpochSecond(EXP), token.getExpiration());
    }
}
