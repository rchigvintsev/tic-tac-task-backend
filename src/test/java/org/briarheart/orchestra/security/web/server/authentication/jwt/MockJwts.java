package org.briarheart.orchestra.security.web.server.authentication.jwt;

import java.time.Duration;
import java.time.Instant;

/**
 * Utility class that provides instances of JWT for tests.
 *
 * @author Roman Chigvintsev
 */
public class MockJwts {
    public static final String DEFAULT_SIGNED_ACCESS_TOKEN_HEADER = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9";
    public static final String DEFAULT_UNSIGNED_ACCESS_TOKEN_HEADER = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0=";
    public static final String DEFAULT_ACCESS_TOKEN_PAYLOAD = "eyJzdWIiOiJKb2huIERvZSJ9";
    public static final String DEFAULT_ACCESS_TOKEN_SIGNATURE = "U1bAs3wp14vdUsh1FE_yMEfKXa69W65i9IFV9AYxUUMTRv65L8Cr"
            + "wvXQU6-jL2n0hydWm43ps9BWvHwDj3z0BQ";

    public static final String DEFAULT_SIGNED_ACCESS_TOKEN_VALUE = DEFAULT_SIGNED_ACCESS_TOKEN_HEADER + "."
            + DEFAULT_ACCESS_TOKEN_PAYLOAD + "." + DEFAULT_ACCESS_TOKEN_SIGNATURE;
    public static final String DEFAULT_UNSIGNED_ACCESS_TOKEN_VALUE = DEFAULT_UNSIGNED_ACCESS_TOKEN_HEADER + "."
            + DEFAULT_ACCESS_TOKEN_PAYLOAD + ".";

    public static final Duration DEFAULT_ACCESS_TOKEN_TIMEOUT = Duration.ofDays(7);

    private MockJwts() {
        //no instance
    }

    /**
     * Creates new JWT with default value and expiration timeout.
     *
     * @return new JWT
     */
    public static Jwt createMock() {
        Instant issuedAt = Instant.now();
        Instant expirationTime = issuedAt.plus(DEFAULT_ACCESS_TOKEN_TIMEOUT);
        return new Jwt.Builder(DEFAULT_SIGNED_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.ISSUED_AT, issuedAt.getEpochSecond())
                .claim(JwtClaim.EXPIRATION_TIME, expirationTime.getEpochSecond())
                .build();
    }

    /**
     * Creates new unsigned JWT ({@link Jwt#getSignature()} returns {@code null} or empty string) with default value
     * and expiration timeout.
     *
     * @return new JWT
     */
    public static Jwt createUnsignedMock() {
        Instant issuedAt = Instant.now();
        Instant expirationTime = issuedAt.plus(DEFAULT_ACCESS_TOKEN_TIMEOUT);
        return new Jwt.Builder(DEFAULT_UNSIGNED_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.ISSUED_AT, issuedAt.getEpochSecond())
                .claim(JwtClaim.EXPIRATION_TIME, expirationTime.getEpochSecond())
                .build();
    }
}
