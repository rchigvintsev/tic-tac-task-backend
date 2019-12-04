package org.briarheart.orchestra.security.oauth2.core.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

/**
 * Google specific implementation of OAuth2 user.
 *
 * @author Roman Chigvintsev
 */
public class GoogleOAuth2User extends DefaultOidcUser implements OAuth2UserAttributeAccessor {
    /**
     * Creates new instance of this class with the given authorities, OIDC ID token, OIDC user information and
     * name attribute key.
     *
     * @param authorities      authorities granted to the user
     * @param idToken          OpenID Connect Core 1.0 ID token containing claims about the user
     * @param userInfo         OpenID Connect Core 1.0 user information containing claims about the user
     * @param nameAttributeKey key used to access the user's &quot;name&quot; from {@link #getAttributes()}
     */
    public GoogleOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            OidcIdToken idToken,
                            OidcUserInfo userInfo,
                            String nameAttributeKey) {
        super(authorities, idToken, userInfo, nameAttributeKey);
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public String getFullName() {
        return super.getFullName();
    }

    @Override
    public String getPicture() {
        return super.getPicture();
    }
}
