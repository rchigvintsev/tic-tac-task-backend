package org.briarheart.orchestra.web.error;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class ApiErrorAttributesTest {
    @Test
    void shouldMapEntityNotFoundExceptionToNotFoundHttpStatus() {
        EntityNotFoundException exception = new EntityNotFoundException("Entity is not found");
        ServerRequest request = mockServerRequest();

        ApiErrorAttributes attributes = new ApiErrorAttributes(false,
                t -> t == EntityNotFoundException.class ? HttpStatus.NOT_FOUND : null);
        attributes.storeErrorInformation(exception, request.exchange());

        Map<String, Object> result = attributes.getErrorAttributes(request, false);
        assertNotNull(result);
        assertEquals(HttpStatus.NOT_FOUND.value(), result.get("status"));
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), result.get("error"));
    }

    @Test
    void shouldIncludeLocalizedMessageInErrorAttributes() {
        String localizedMessage = "Что-то пошло не так";
        Exception exception = new Exception("Something went wrong") {
            @Override
            public String getLocalizedMessage() {
                return localizedMessage;
            }
        };
        ServerRequest request = mockServerRequest();

        ApiErrorAttributes attributes = new ApiErrorAttributes(false, t -> null);
        attributes.storeErrorInformation(exception, request.exchange());

        Map<String, Object> result = attributes.getErrorAttributes(request, false);
        assertNotNull(result);
        assertEquals(localizedMessage, result.get("localizedMessage"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldIncludeFieldErrorsInErrorAttributes() {
        List fieldErrors = new ArrayList();
        fieldErrors.add(new FieldError("test", "value", "Value must not be null"));

        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.hasErrors()).thenReturn(true);
        when(exception.hasFieldErrors()).thenReturn(true);
        when(exception.getAllErrors()).thenReturn(fieldErrors);
        when(exception.getFieldErrors()).thenReturn(fieldErrors);

        ServerRequest request = mockServerRequest();

        ApiErrorAttributes attributes = new ApiErrorAttributes(false, t -> null);
        attributes.storeErrorInformation(exception, request.exchange());

        Map<String, Object> result = attributes.getErrorAttributes(request, false);
        assertNotNull(result);
        assertNotNull(result.get("fieldErrors"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldRemoveMessageFromErrorAttributesWhenFieldErrorsExist() {
        List fieldErrors = new ArrayList();
        fieldErrors.add(new FieldError("test", "value", "Value must not be null"));

        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.hasErrors()).thenReturn(true);
        when(exception.hasFieldErrors()).thenReturn(true);
        when(exception.getAllErrors()).thenReturn(fieldErrors);
        when(exception.getFieldErrors()).thenReturn(fieldErrors);

        ServerRequest request = mockServerRequest();

        ApiErrorAttributes attributes = new ApiErrorAttributes(false, t -> null);
        attributes.storeErrorInformation(exception, request.exchange());

        Map<String, Object> result = attributes.getErrorAttributes(request, false);
        assertNotNull(result);
        assertNull(result.get("message"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void shouldRemoveErrorsFromErrorAttributesWhenOnlyFieldErrorsExist() {
        List fieldErrors = new ArrayList();
        fieldErrors.add(new FieldError("test", "value", "Value must not be null"));

        WebExchangeBindException exception = mock(WebExchangeBindException.class);
        when(exception.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.hasErrors()).thenReturn(true);
        when(exception.hasFieldErrors()).thenReturn(true);
        when(exception.getAllErrors()).thenReturn(fieldErrors);
        when(exception.getFieldErrors()).thenReturn(fieldErrors);

        ServerRequest request = mockServerRequest();

        ApiErrorAttributes attributes = new ApiErrorAttributes(false, t -> null);
        attributes.storeErrorInformation(exception, request.exchange());

        Map<String, Object> result = attributes.getErrorAttributes(request, false);
        assertNotNull(result);
        assertNull(result.get("errors"));
    }

    @Test
    void shouldThrowExceptionOnConstructWhenHttpStatusExceptionTypeMapperIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new ApiErrorAttributes(false, null),
                "Exception type to HTTP status mapper must not be null");
    }

    @Test
    void shouldThrowExceptionOnErrorAttributesGetWhenServerRequestIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiErrorAttributes(false, t -> null).getErrorAttributes(null, false),
                "Server request must not be null");
    }

    private ServerRequest mockServerRequest() {
        Map<String, Object> requestAttributes = new HashMap<>();

        ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
        when(httpRequest.getId()).thenReturn("1");

        ServerWebExchange webExchange = mock(ServerWebExchange.class);
        when(webExchange.getAttributes()).thenReturn(requestAttributes);
        when(webExchange.getRequest()).thenReturn(httpRequest);

        ServerRequest request = mock(ServerRequest.class);
        when(request.path()).thenReturn("/test");
        when(request.exchange()).thenReturn(webExchange);
        when(request.attribute(anyString()))
                .thenAnswer(args -> Optional.of(requestAttributes.get(args.getArgument(0, String.class))));
        return request;
    }
}
