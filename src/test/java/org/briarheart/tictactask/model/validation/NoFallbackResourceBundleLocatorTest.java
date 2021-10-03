package org.briarheart.tictactask.model.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class NoFallbackResourceBundleLocatorTest {
    private NoFallbackResourceBundleLocator locator;

    @BeforeEach
    void setUp() {
        locator = new NoFallbackResourceBundleLocator();
    }

    @Test
    void shouldGetResourceBundleForEnglishLocale() {
        ResourceBundle resourceBundle = locator.getResourceBundle(Locale.ENGLISH);
        assertNotNull(resourceBundle);
        String notNullMessage = resourceBundle.getString("javax.validation.constraints.NotNull.message");
        assertEquals("Value must not be null", notNullMessage);
    }

    @Test
    void shouldGetResourceBundleForRussianLocale() {
        ResourceBundle resourceBundle = locator.getResourceBundle(Locale.forLanguageTag("ru"));
        assertNotNull(resourceBundle);
        String notNullMessage = resourceBundle.getString("javax.validation.constraints.NotNull.message");
        assertEquals("Значение должно быть задано", notNullMessage);
    }

    @Test
    void shouldFallbackToResourceBundleForEnglishLocaleWhenLocaleIsNotSupported() {
        ResourceBundle resourceBundle = locator.getResourceBundle(Locale.forLanguageTag("tlh"));
        assertNotNull(resourceBundle);
        String notNullMessage = resourceBundle.getString("javax.validation.constraints.NotNull.message");
        assertEquals("Value must not be null", notNullMessage);
    }

    @Test
    void shouldThrowExceptionOnCreateWhenResourceBundleNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new NoFallbackResourceBundleLocator(null));
    }

    @Test
    void shouldThrowExceptionOnResourceBundleGetWhenLocaleIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> locator.getResourceBundle(null));
        assertEquals("Locale must not be null", e.getMessage());
    }
}
