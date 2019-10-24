package org.briarheart.orchestra.security.oauth2.core.user;

import java.util.Map;

/**
 * An &quot;accessor&quot; for OAuth 2 user attributes.
 *
 * @author Roman Chigvintsev
 */
public interface OAuth2UserAttributeAccessor {
    Map<String, Object> getAttributes();

    String getEmail();

    String getFullName();

    String getPicture();
}
