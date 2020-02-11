package org.briarheart.orchestra.controller;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.lang.reflect.Executable;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
@RestController
public class TestController {
    @PostMapping("/webExchangeBindException")
    public Mono<String> throwWebExchangeBindException() {
        Executable executableMock = mock(Executable.class);

        MethodParameter methodParameterMock = mock(MethodParameter.class);
        when(methodParameterMock.getExecutable()).thenReturn(executableMock);

        BindingResult bindingResultMock = mock(BindingResult.class);
        FieldError fieldError = new FieldError("TestObject", "testField", "Test constraint is violated!");
        when(bindingResultMock.getFieldErrorCount()).thenReturn(1);
        when(bindingResultMock.getErrorCount()).thenReturn(1);
        when(bindingResultMock.getFieldErrors()).thenReturn(List.of(fieldError));
        when(bindingResultMock.getAllErrors()).thenReturn(List.of(fieldError));

        throw new WebExchangeBindException(methodParameterMock, bindingResultMock);
    }
}
