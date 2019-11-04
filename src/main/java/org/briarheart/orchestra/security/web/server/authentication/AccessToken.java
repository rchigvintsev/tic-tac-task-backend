package org.briarheart.orchestra.security.web.server.authentication;

import java.io.Serializable;
import java.time.Instant;

/**
 * Token to access application resources.
 *
 * @author Roman Chigvintsev
 */
public interface AccessToken extends Serializable {
    String getTokenValue();

    String getHeader();

    String getPayload();

    String getSignature();

    String getSubject();

    Instant getIssuedAt();

    Instant getExpiration();
}
