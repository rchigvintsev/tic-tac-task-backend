package org.briarheart.orchestra.security.web.server.authentication;

import java.io.Serializable;

/**
 * Token to access application resources.
 *
 * @author Roman Chigvintsev
 */
public interface AccessToken extends Serializable {
    String getTokenValue();

    String getSubject();
}
