package org.briarheart.orchestra.controller;

import lombok.Getter;
import lombok.Setter;
import org.briarheart.orchestra.model.validation.constraints.NotPast;
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
            if (NotPast.class.getSimpleName().equals(globalError.getCode())) {
                fieldErrors.putAll(handleNotPastConstraintViolation(globalError, e));
            } else {
                globalErrorMessages.add(globalError.getDefaultMessage());
            }
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

    private Map<String, String> handleNotPastConstraintViolation(ObjectError error, WebExchangeBindException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        Object target = e.getTarget();
        if (target != null) {
            NotPast notPast = target.getClass().getAnnotation(NotPast.class);
            Object date = e.getRawFieldValue(notPast.dateFieldName());
            if (date != null) {
                fieldErrors.put(notPast.dateFieldName(), error.getDefaultMessage());
            }
            Object time = e.getRawFieldValue(notPast.timeFieldName());
            if (time != null) {
                fieldErrors.put(notPast.timeFieldName(), error.getDefaultMessage());
            }
        }
        return fieldErrors;
    }
}
