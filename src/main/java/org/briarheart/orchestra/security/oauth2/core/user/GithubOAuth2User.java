package org.briarheart.orchestra.security.oauth2.core.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Github specific implementation of OAuth2 user.
 *
 * @author Roman Chigvintsev
 */
public class GithubOAuth2User extends DefaultOAuth2User implements OAuth2UserAttributeAccessor {
    /**
     * Creates new instance of this class with the given authorities, attributes and name attribute key.
     *
     * @param authorities authorities granted to the user
     * @param attributes attributes about the user
     * @param nameAttributeKey key used to access the user's &quot;name&quot; from {@link #getAttributes()}
     */
    public GithubOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
    }

    @Override
    public String getEmail() {
        return Objects.toString(getAttributes().get("email"), null);
    }

    @Override
    public String getFullName() {
        return Objects.toString(getAttributes().get("name"), null);
    }

    @Override
    public String getPicture() {
        return Objects.toString(getAttributes().get("avatar_url"), null);
    }
}

