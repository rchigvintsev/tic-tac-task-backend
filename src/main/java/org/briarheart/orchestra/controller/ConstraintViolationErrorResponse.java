package org.briarheart.orchestra.controller;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.springframework.util.Assert;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Set of error messages obtained from {@link ConstraintViolationException}.
 *
 * @author Roman Chigvintsev
 */
public class ConstraintViolationErrorResponse extends ErrorResponse {
    @Getter
    private final List<String> messages;

    /**
     * Creates new instance of this class with the given {@link ConstraintViolationException}.
     *
     * @param e exception (must not be {@code null})
     */
    public ConstraintViolationErrorResponse(ConstraintViolationException e) {
        Assert.notNull(e, "Exception must not be null");
        List<String> messages = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(toList());
        this.messages = ImmutableList.copyOf(messages);
    }
}
