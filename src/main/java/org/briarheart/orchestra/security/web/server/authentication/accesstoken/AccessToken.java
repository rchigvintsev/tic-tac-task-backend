package org.briarheart.orchestra.security.web.server.authentication.accesstoken;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * Token to access application resources.
 *
 * @author Roman Chigvintsev
 */
public interface AccessToken extends Serializable {
    String getTokenValue();

    String getSubject();

    Instant getIssuedAt();

    Instant getExpiration();

    Map<String, Object> getClaims();
}
