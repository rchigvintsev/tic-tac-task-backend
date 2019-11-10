package org.briarheart.orchestra.security.web.server.authentication.jwt;

/**
 * Registered claim names defined by the JSON Web Token (JWT) specification.
 *
 * @author Roman Chigvintsev
 */
public enum JwtClaimNames {
    NAME("name"),
    PICTURE("picture");

    private final String name;

    /**
     * Creates new instance of this class with the given claim name.
     *
     * @param name claim name (must not be {@code null} or empty)
     */
    JwtClaimNames(String name) {
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
