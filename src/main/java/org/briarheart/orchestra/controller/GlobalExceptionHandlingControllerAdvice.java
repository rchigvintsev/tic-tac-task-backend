package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolationException;

/**
 * This advice handles exceptions arising in controllers.
 *
 * @author Roman Chigvintsev
 */
@ControllerAdvice
public class GlobalExceptionHandlingControllerAdvice {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlingControllerAdvice.class);

    /**
     * Handles {@link ConstraintViolationException} providing client with concise and meaningful error messages.
     *
     * @param e exception (must not be {@code null})
     * @return instance of {@link ConstraintViolationErrorResponse}
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ConstraintViolationErrorResponse handleConstraintViolationException(ConstraintViolationException e) {
        Assert.notNull(e, "Exception must not be null");
        logger.debug(e.getMessage(), e);
        return new ConstraintViolationErrorResponse(e);
    }

    /**
     * Maps {@link EntityNotFoundException} to response with status code "404 Not found" and body containing error
     * message.
     *
     * @param e exception (must not be {@code null})
     * @return instance of {@link ErrorResponse}
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorResponse handleEntityNotFoundException(EntityNotFoundException e) {
        Assert.notNull(e, "Exception must not be null");
        logger.debug(e.getMessage(), e);
        ErrorResponse response = new ErrorResponse();
        response.setMessage(e.getMessage());
        return response;
    }
}
