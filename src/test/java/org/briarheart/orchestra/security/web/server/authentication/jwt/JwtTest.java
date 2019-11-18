package org.briarheart.orchestra.security.web.server.authentication.jwt;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Roman Chigvintsev
 */
class JwtTest {
    @Test
    void shouldReturnTokenHeader() {
        String header = new Jwt.Builder(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE).build().getHeader();
        assertEquals(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER, header);
    }

    @Test
    void shouldReturnNullOnHeaderGetWhenTokenIsMalformed() {
        String header = new Jwt.Builder(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER).build().getHeader();
        assertNull(header);
    }

    @Test
    void shouldReturnTokenPayload() {
        String payload = new Jwt.Builder(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE).build().getPayload();
        assertEquals(MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD, payload);
    }

    @Test
    void shouldReturnNullOnPayloadGetWhenTokenIsMalformed() {
        String payload1 = new Jwt.Builder(MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD).build().getPayload();
        assertNull(payload1);
        String tokenValue = MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_HEADER + "." + MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD;
        String payload2 = new Jwt.Builder(tokenValue).build().getPayload();
        assertNull(payload2);
    }

    @Test
    void shouldReturnTokenSignature() {
        String signature = new Jwt.Builder(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE).build().getSignature();
        assertEquals(MockJwts.DEFAULT_ACCESS_TOKEN_SIGNATURE, signature);
    }

    @Test
    void shouldReturnNullOnSignatureGetWhenTokenIsMalformed() {
        assertNull(new Jwt.Builder(MockJwts.DEFAULT_UNSIGNED_ACCESS_TOKEN_HEADER).build().getSignature());
        assertNull(new Jwt.Builder(MockJwts.DEFAULT_UNSIGNED_ACCESS_TOKEN_HEADER + "."
                + MockJwts.DEFAULT_ACCESS_TOKEN_PAYLOAD).build().getSignature());
    }

    @Test
    void shouldReturnIssuedAtClaim() {
        final long IAT = 1570969232L;
        Jwt token = new Jwt.Builder(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.ISSUED_AT, IAT)
                .build();
        assertEquals(Instant.ofEpochSecond(IAT), token.getIssuedAt());
    }

    @Test
    void shouldReturnExpirationClaim() {
        final long EXP = 9214159449945032L;
        Jwt token = new Jwt.Builder(MockJwts.DEFAULT_SIGNED_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.EXPIRATION_TIME, EXP)
                .build();
        assertEquals(Instant.ofEpochSecond(EXP), token.getExpiration());
    }
}
