package org.briarheart.orchestra.util;

import org.springframework.core.MethodParameter;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.lang.reflect.Constructor;
import java.util.Collections;

/**
 * Utility class to create special kinds of exceptions.
 *
 * @author Roman Chigvintsev
 */
public class Errors {
    private static final Constructor<Dummy> DUMMY_CONSTRUCTOR;

    static {
        try {
            DUMMY_CONSTRUCTOR = Dummy.class.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Default dummy constructor is not found", e);
        }
    }

    private Errors() {
        //no instance
    }

    /**
     * Creates instance of {@link WebExchangeBindException} with one field error.
     *
     * @param fieldName field name
     * @param errorMessage error message
     * @return instance of {@link WebExchangeBindException}
     */
    public static WebExchangeBindException createFieldError(String fieldName, String errorMessage) {
        WebExchangeBindException e = new WebExchangeBindException(new MethodParameter(DUMMY_CONSTRUCTOR, -1),
                new MapBindingResult(Collections.emptyMap(), "dummy"));
        e.rejectValue(fieldName, "", errorMessage);
        return e;
    }

    private static class Dummy {
    }
}
