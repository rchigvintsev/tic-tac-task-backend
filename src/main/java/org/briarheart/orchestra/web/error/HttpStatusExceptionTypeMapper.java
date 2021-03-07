package org.briarheart.orchestra.web.error;

import org.springframework.http.HttpStatus;

/**
 * Strategy interface that can be used to provide a mapping between exception types and HTTP statuses.
 *
 * @author Roman Chigvintsev
 */
@FunctionalInterface
public interface HttpStatusExceptionTypeMapper {
    /**
     * Returns HTTP status corresponding to the given exception type or {@code null}.
     *
     * @param exceptionType exception type (must not be {@code null})
     * @return instance of {@link HttpStatus} or {@code null} if there is no HTTP status associated with the given
     * exception type
     */
    HttpStatus getHttpStatus(Class<? extends Throwable> exceptionType);
}
