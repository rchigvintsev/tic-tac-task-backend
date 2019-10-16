package org.briarheart.orchestra.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class EmptyUserTest {
    @Test
    void shouldIgnoreEmail() {
        User.EMPTY.setEmail("example@mail.com");
        assertNull(User.EMPTY.getEmail());
    }

    @Test
    void shouldIgnoreVersion() {
        User.EMPTY.setVersion(1L);
        assertEquals(0L, User.EMPTY.getVersion());
    }

    @Test
    void shouldIgnoreFullName() {
        User.EMPTY.setFullName("John Doe");
        assertNull(User.EMPTY.getFullName());
    }

    @Test
    void shouldIgnoreImageUrl() {
        User.EMPTY.setImageUrl("http://example.com/image");
        assertNull(User.EMPTY.getImageUrl());
    }

    @Test
    void shouldBeEqualOnlyToItself() {
        assertEquals(User.EMPTY, User.EMPTY);
        assertNotEquals(User.EMPTY, new User());
    }

    @Test
    void shouldReturnOneForHashCode() {
        assertEquals(1, User.EMPTY.hashCode());
    }
}
