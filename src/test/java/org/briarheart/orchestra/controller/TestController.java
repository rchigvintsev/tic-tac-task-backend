package org.briarheart.orchestra.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
@RestController
public class TestController {
    @PostMapping("/constraintViolationException")
    public Mono<String> throwConstraintViolationException() {
        ConstraintViolation<?> violationMock = mock(ConstraintViolation.class);
        when(violationMock.getMessage()).thenReturn("Test constraint is violated!");
        throw new ConstraintViolationException("Test constraint violation", Set.of(violationMock));
    }
}
