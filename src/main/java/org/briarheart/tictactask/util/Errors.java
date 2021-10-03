package org.briarheart.tictactask.util;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
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
     * @param fieldValue field value
     * @param errorMessage error message
     * @return instance of {@link WebExchangeBindException}
     */
    public static WebExchangeBindException createFieldError(String fieldName, String fieldValue, String errorMessage) {
        MethodParameter parameter = new MethodParameter(DUMMY_CONSTRUCTOR, -1);
        BindingResult bindingResult = new MapBindingResult(Collections.singletonMap(fieldName, fieldValue), "dummy");
        WebExchangeBindException e = new WebExchangeBindException(parameter, bindingResult);
        e.rejectValue(fieldName, "", errorMessage);
        return e;
    }

    private static class Dummy {
    }
}
