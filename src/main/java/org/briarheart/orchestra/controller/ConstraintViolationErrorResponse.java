package org.briarheart.orchestra.controller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        List<String> globalErrorMessages = new ArrayList<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (ObjectError globalError : e.getGlobalErrors()) {
            globalErrorMessages.add(globalError.getDefaultMessage());
        }

        if (!globalErrorMessages.isEmpty()) {
            setErrors(globalErrorMessages);
        }

        if (e.getFieldErrorCount() > 0) {
            e.getFieldErrors()
                    .forEach(fieldError -> fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));
        }

        if (!fieldErrors.isEmpty()) {
            this.fieldErrors = fieldErrors;
        }
    }
}
