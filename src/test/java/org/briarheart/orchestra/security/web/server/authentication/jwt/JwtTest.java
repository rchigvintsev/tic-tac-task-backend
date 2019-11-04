package org.briarheart.orchestra.security.web.server.authentication.jwt;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Roman Chigvintsev
 */
class JwtTest {
    private static final String ACCESS_TOKEN_HEADER = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9";
    private static final String ACCESS_TOKEN_PAYLOAD = "eyJzdWIiOiJKb2huIERvZSJ9";
    private static final String ACCESS_TOKEN_SIGNATURE = "U1bAs3wp14vdUsh1FE_yMEfKXa69W65i9IFV9AYxUUMTRv65L8CrwvXQU6-"
            + "jL2n0hydWm43ps9BWvHwDj3z0BQ";
    private static final String ACCESS_TOKEN_VALUE = ACCESS_TOKEN_HEADER + "." + ACCESS_TOKEN_PAYLOAD + "."
            + ACCESS_TOKEN_SIGNATURE;

    @Test
    void shouldReturnTokenHeader() {
        String header = new Jwt.Builder(ACCESS_TOKEN_VALUE).build().getHeader();
        assertEquals(ACCESS_TOKEN_HEADER, header);
    }

    @Test
    void shouldReturnNullOnHeaderGetWhenTokenIsMalformed() {
        String header = new Jwt.Builder(ACCESS_TOKEN_HEADER).build().getHeader();
        assertNull(header);
    }

    @Test
    void shouldReturnTokenPayload() {
        String payload = new Jwt.Builder(ACCESS_TOKEN_VALUE).build().getPayload();
        assertEquals(ACCESS_TOKEN_PAYLOAD, payload);
    }

    @Test
    void shouldReturnNullOnPayloadGetWhenTokenIsMalformed() {
        String payload1 = new Jwt.Builder(ACCESS_TOKEN_PAYLOAD).build().getPayload();
        assertNull(payload1);
        String payload2 = new Jwt.Builder(ACCESS_TOKEN_HEADER + "." + ACCESS_TOKEN_PAYLOAD).build().getPayload();
        assertNull(payload2);
    }

    @Test
    void shouldReturnTokenSignature() {
        String signature = new Jwt.Builder(ACCESS_TOKEN_VALUE).build().getSignature();
        assertEquals(ACCESS_TOKEN_SIGNATURE, signature);
    }

    @Test
    void shouldReturnNullOnSignatureGetWhenTokenIsMalformed() {
        String signature = new Jwt.Builder(ACCESS_TOKEN_SIGNATURE).build().getSignature();
        assertNull(signature);
    }

    @Test
    void shouldReturnIssuedAtClaim() {
        final long IAT = 1570969232L;
        Jwt token = new Jwt.Builder(ACCESS_TOKEN_VALUE)
                .claims(Map.of("iat", IAT))
                .build();
        assertEquals(Instant.ofEpochSecond(IAT), token.getIssuedAt());
    }

    @Test
    void shouldReturnExpirationClaim() {
        final long EXP = 9214159449945032L;
        Jwt token = new Jwt.Builder(ACCESS_TOKEN_VALUE)
                .claims(Map.of("exp", EXP))
                .build();
        assertEquals(Instant.ofEpochSecond(EXP), token.getExpiration());
    }
}
