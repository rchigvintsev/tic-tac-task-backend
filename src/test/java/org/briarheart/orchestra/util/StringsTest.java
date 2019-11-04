package org.briarheart.orchestra.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Roman Chigvintsev
 */
class StringsTest {
    private StringsTest() {
        //no instance
    }

    @Test
    void shouldReturnTrueOnHasTextForTextContainingStrings() {
        assertTrue(Strings.hasText("foo", "bar"));
    }

    @Test
    void shouldReturnFalseOnHasTextWhenAtLeastOneStringIsBlank() {
        assertFalse(Strings.hasText("foo", " "));
    }

    @Test
    void shouldReturnFalseOnHasTextWhenAtLeastOneStringIsNull() {
        assertFalse(Strings.hasText("foo", null));
    }

    @Test
    void shouldReturnFalseOnHasTextWhenStringArrayIsEmpty() {
        String[] strings = new String[0];
        assertFalse(Strings.hasText(strings));
    }

    @Test
    void shouldReturnFalseOnHasTextWhenStringArrayIsNull() {
        assertFalse(Strings.hasText((String[]) null));
    }
}
