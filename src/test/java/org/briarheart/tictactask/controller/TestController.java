package org.briarheart.tictactask.controller;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
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
        ObjectError error = new ObjectError("TestObject", "Web exchange binding error occurred!");
        when(bindingResultMock.getGlobalErrorCount()).thenReturn(1);
        when(bindingResultMock.getGlobalErrors()).thenReturn(List.of(error));
        when(bindingResultMock.getErrorCount()).thenReturn(1);
        when(bindingResultMock.getAllErrors()).thenReturn(List.of(error));

        throw new WebExchangeBindException(methodParameterMock, bindingResultMock);
    }
}
