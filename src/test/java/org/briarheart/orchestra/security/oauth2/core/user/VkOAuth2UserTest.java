package org.briarheart.orchestra.security.oauth2.core.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Roman Chigvintsev
 */
class VkOAuth2UserTest {
    @Test
    void shouldReturnFullName() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        Map<String, Object> attributes = Map.of("id", 1, "first_name", "John", "last_name", "Doe");
        VkOAuth2User user = new VkOAuth2User(authorities, attributes, "id");
        assertEquals("John Doe", user.getFullName());
    }

    @Test
    void shouldReturnCorrectFullNameWhenFirstNameIsNotSet() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        Map<String, Object> attributes = Map.of("id", 1, "last_name", "Doe");
        VkOAuth2User user = new VkOAuth2User(authorities, attributes, "id");
        assertEquals("Doe", user.getFullName());
    }

    @Test
    void shouldReturnCorrectFullNameWhenFirstNameIsEmpty() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        Map<String, Object> attributes = Map.of("id", 1, "first_name", "", "last_name", "Doe");
        VkOAuth2User user = new VkOAuth2User(authorities, attributes, "id");
        assertEquals("Doe", user.getFullName());
    }

    @Test
    void shouldReturnCorrectFullNameWhenLastNameIsNotSet() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        Map<String, Object> attributes = Map.of("id", 1, "first_name", "John");
        VkOAuth2User user = new VkOAuth2User(authorities, attributes, "id");
        assertEquals("John", user.getFullName());
    }

    @Test
    void shouldReturnCorrectFullNameWhenLastNameIsEmpty() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        Map<String, Object> attributes = Map.of("id", 1, "first_name", "John", "last_name", "");
        VkOAuth2User user = new VkOAuth2User(authorities, attributes, "id");
        assertEquals("John", user.getFullName());
    }

    @Test
    void shouldReturnNullForFullNameWhenNeitherFirstNameNorLastNameIsSet() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("user"));
        Map<String, Object> attributes = Map.of("id", 1);
        VkOAuth2User user = new VkOAuth2User(authorities, attributes, "id");
        assertNull(user.getFullName());
    }
}
