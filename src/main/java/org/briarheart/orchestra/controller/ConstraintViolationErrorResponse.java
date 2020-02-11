package org.briarheart.orchestra.controller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Error response that is used in case some entity constraints were violated. Provides {@code <field name> ->
 * <error message>} mapping.
 *
 * @author Roman Chigvintsev
 */
public class ConstraintViolationErrorResponse extends ErrorResponse {
    @Getter
    @Setter
    private Map<String, String> fieldErrors;

    /**
     * Creates new instance of this class with the given {@link WebExchangeBindException}.
     *
     * @param e exception (must not be {@code null})
     */
    public ConstraintViolationErrorResponse(WebExchangeBindException e) {
        Assert.notNull(e, "Exception must not be null");
        setErrors(e.getGlobalErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList()));
        if (e.getFieldErrorCount() > 0) {
            this.fieldErrors = new HashMap<>();
            e.getFieldErrors()
                    .forEach(fieldError -> this.fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        }
    }
}
