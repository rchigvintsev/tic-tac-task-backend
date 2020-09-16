package org.briarheart.orchestra.controller;

import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class ConstraintViolationErrorResponseTest {
    @Test
    void shouldCreateResponseWithGlobalErrorMessages() {
        ObjectError error = new ObjectError("test", "Something went wrong");
        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getGlobalErrors()).thenReturn(List.of(error));
        ConstraintViolationErrorResponse response = new ConstraintViolationErrorResponse(exception);
        assertEquals(List.of(requireNonNull(error.getDefaultMessage())), response.getErrors());
    }

    @Test
    void shouldCreateResponseWithFieldErrorMessages() {
        FieldError error = new FieldError("test", "field", "Something went wrong");
        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getGlobalErrors()).thenReturn(List.of());
        when(exception.getFieldErrorCount()).thenReturn(1);
        when(exception.getFieldErrors()).thenReturn(List.of(error));
        ConstraintViolationErrorResponse response = new ConstraintViolationErrorResponse(exception);
        assertEquals(Map.of(error.getField(), requireNonNull(error.getDefaultMessage())), response.getFieldErrors());
    }
}
