package org.briarheart.orchestra.model.validation;

import io.jsonwebtoken.lang.Assert;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.util.privilegedactions.GetClassLoader;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

/**
 * Simplified version of {@link org.hibernate.validator.resourceloading.PlatformResourceBundleLocator} that never
 * fallbacks to system locale loading messages from default (without language postfix) bundle file.
 *
 * @author Roman Chigvintsev
 */
@Slf4j
@RequiredArgsConstructor
public class NoFallbackResourceBundleLocator implements ResourceBundleLocator {
    @NonNull
    private final String bundleName;

    /**
     * Creates new instance of this class with default bundle name
     * ({@link AbstractMessageInterpolator#USER_VALIDATION_MESSAGES}).
     */
    public NoFallbackResourceBundleLocator() {
        this(AbstractMessageInterpolator.USER_VALIDATION_MESSAGES);
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        Assert.notNull(locale, "Locale must not be null");

        ResourceBundle resourceBundle = null;

        ClassLoader classLoader = runPrivilegedAction(GetClassLoader.fromContext());
        if (classLoader != null) {
            resourceBundle = loadBundle(classLoader, "thread context class loader", locale);
        }

        if (resourceBundle == null) {
            classLoader = runPrivilegedAction(GetClassLoader.fromClass(getClass()));
            resourceBundle = loadBundle(classLoader, "validator class loader", locale);
        }

        if (log.isDebugEnabled()) {
            log.debug("Bundle \"{}\" is {}", bundleName, resourceBundle != null ? "found" : "not found");
        }
        return resourceBundle;
    }

    private ResourceBundle loadBundle(ClassLoader classLoader, String classLoaderName, Locale locale) {
        ResourceBundle resourceBundle = null;
        Control control = Control.getNoFallbackControl(Control.FORMAT_DEFAULT);
        try {
            resourceBundle = ResourceBundle.getBundle(bundleName, locale, classLoader, control);
        } catch (MissingResourceException e) {
            log.debug("Bundle \"{}\" is not found by {}", bundleName, classLoaderName);
        }
        return resourceBundle;
    }

    private <T> T runPrivilegedAction(PrivilegedAction<T> action) {
        return System.getSecurityManager() != null ? AccessController.doPrivileged(action) : action.run();
    }
}
