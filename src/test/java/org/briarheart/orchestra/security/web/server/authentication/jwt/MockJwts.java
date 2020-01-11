package org.briarheart.orchestra.security.web.server.authentication.jwt;

import java.time.Duration;
import java.time.Instant;

/**
 * Utility class that provides instances of JWT for tests.
 *
 * @author Roman Chigvintsev
 */
public class MockJwts {
    public static final String DEFAULT_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9."
            + "eyJzdWIiOiJKb2huIERvZSJ9."
            + "U1bAs3wp14vdUsh1FE_yMEfKXa69W65i9IFV9AYxUUMTRv65L8CrwvXQU6-jL2n0hydWm43ps9BWvHwDj3z0BQ";
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
        return new Jwt.Builder(DEFAULT_ACCESS_TOKEN_VALUE)
                .claim(JwtClaim.ISSUED_AT, issuedAt.getEpochSecond())
                .claim(JwtClaim.EXPIRATION_TIME, expirationTime.getEpochSecond())
                .build();
    }
}
