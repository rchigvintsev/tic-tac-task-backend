package org.briarheart.orchestra.security.web.server.authentication.jwt;

/**
 * Registered claims defined by the JSON Web Token (JWT) specification.
 *
 * @author Roman Chigvintsev
 */
public enum JwtClaim {
    ISSUED_AT("iat"),
    EXPIRATION_TIME("exp"),
    FULL_NAME("name"),
    PROFILE_PICTURE_URL("picture");

    private final String name;

    /**
     * Creates new instance of this class with the given claim name.
     *
     * @param name claim name (must not be {@code null} or empty)
     */
    JwtClaim(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
