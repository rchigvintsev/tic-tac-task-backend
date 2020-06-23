package org.briarheart.orchestra.controller;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.model.validation.constraints.NotPast;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Test
    void shouldCreateResponseWithDateFieldErrorMessageInCaseNotPastConstraintViolation() {
        ObjectError error = new ObjectError("test", new String[] {"NotPast"}, null, "Date must not be in past");
        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        OnlyDate target = new OnlyDate(LocalDate.now());
        when(exception.getTarget()).thenReturn(target);
        when(exception.getRawFieldValue("date")).thenReturn(target.date);
        when(exception.getGlobalErrors()).thenReturn(List.of(error));
        ConstraintViolationErrorResponse response = new ConstraintViolationErrorResponse(exception);
        assertEquals(Map.of("date", requireNonNull(error.getDefaultMessage())), response.getFieldErrors());
    }

    @Test
    void shouldCreateResponseWithTimeFieldErrorMessageInCaseNotPastConstraintViolation() {
        ObjectError error = new ObjectError("test", new String[] {"NotPast"}, null, "Time must not be in past");
        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        OnlyTime target = new OnlyTime(LocalTime.now());
        when(exception.getTarget()).thenReturn(target);
        when(exception.getRawFieldValue("time")).thenReturn(target.time);
        when(exception.getGlobalErrors()).thenReturn(List.of(error));
        ConstraintViolationErrorResponse response = new ConstraintViolationErrorResponse(exception);
        assertEquals(Map.of("time", requireNonNull(error.getDefaultMessage())), response.getFieldErrors());
    }

    @NotPast(dateFieldName = "date")
    @RequiredArgsConstructor
    private static class OnlyDate {
        private final LocalDate date;
    }

    @NotPast(timeFieldName = "time")
    @RequiredArgsConstructor
    private static class OnlyTime {
        private final LocalTime time;
    }
}
