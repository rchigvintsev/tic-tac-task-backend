package org.briarheart.orchestra.controller;

import org.mockito.Mockito;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

import static org.mockito.Mockito.mock;

/**
 * @author Roman Chigvintsev
 */
@Controller
public class TestController {
    @PostMapping("/constraintViolationException")
    public String throwConstraintViolationException() {
        ConstraintViolation<?> violationMock = mock(ConstraintViolation.class);
        Mockito.when(violationMock.getMessage()).thenReturn("Test constraint is violated!");
        throw new ConstraintViolationException("Test constraint violation", Set.of(violationMock));
    }
}
