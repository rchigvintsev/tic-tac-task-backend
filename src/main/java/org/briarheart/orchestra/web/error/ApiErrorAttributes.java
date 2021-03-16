package org.briarheart.orchestra.web.error;

import org.briarheart.orchestra.LocalizedRuntimeException;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link org.springframework.boot.web.reactive.error.ErrorAttributes} for error responses from
 * REST API. The key differences from {@link DefaultErrorAttributes} are the following:
 * <ul>
 *     <li>added optional "localizedMessage" attribute for localizes error messages;</li>
 *     <li>added optional "filedErrors" attribute for simplified representation of field errors.</li>
 * </ul>
 *
 * @author Roman Chigvintsev
 */
public class ApiErrorAttributes extends DefaultErrorAttributes {
    private final HttpStatusExceptionTypeMapper httpStatusExceptionMapper;

    /**
     * Creates new instance of this class with the given include exception flag and
     * {@link HttpStatusExceptionTypeMapper}.
     *
     * @param includeException              indicates whether exception must be included in error attributes
     * @param httpStatusExceptionTypeMapper exception type to HTTP status mapper (must not be {@link null})
     */
    public ApiErrorAttributes(boolean includeException, HttpStatusExceptionTypeMapper httpStatusExceptionTypeMapper) {
        super(includeException);
        Assert.notNull(httpStatusExceptionTypeMapper, "Exception type to HTTP status mapper must not be null");
        this.httpStatusExceptionMapper = httpStatusExceptionTypeMapper;
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        Assert.notNull(request, "Server request must not be null");

        Map<String, Object> errorAttributes = new LinkedHashMap<>(super.getErrorAttributes(request, includeStackTrace));
        Throwable error = getError(request);
        HttpStatus httpStatus = httpStatusExceptionMapper.getHttpStatus(error.getClass());
        if (httpStatus != null) {
            errorAttributes.put("status", httpStatus.value());
            errorAttributes.put("error", httpStatus.getReasonPhrase());
        }
        if (error instanceof LocalizedRuntimeException && StringUtils.hasLength(error.getLocalizedMessage())) {
            errorAttributes.put("localizedMessage", error.getLocalizedMessage());
        }
        if (error instanceof BindingResult) {
            handleBindingResult(errorAttributes, (BindingResult) error);
        }
        return errorAttributes;
    }

    @SuppressWarnings("unchecked")
    protected void handleBindingResult(Map<String, Object> errorAttributes, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            // Usually the message contains information related to server side only
            errorAttributes.remove("message");
            List<ObjectError> allErrors = (List<ObjectError>) errorAttributes.remove("errors");
            if (allErrors != null) {
                List<Map<String, Object>> fieldErrors = new ArrayList<>();
                List<ObjectError> otherErrors = new ArrayList<>();
                for (ObjectError error : allErrors) {
                    if (error instanceof FieldError) {
                        fieldErrors.add(getFieldErrorAttributes((FieldError) error));
                    } else {
                        otherErrors.add(error);
                    }
                }
                errorAttributes.put("fieldErrors", fieldErrors);
                if (!otherErrors.isEmpty()) {
                    errorAttributes.put("errors", otherErrors);
                }
            }
        }
    }

    protected Map<String, Object> getFieldErrorAttributes(FieldError error) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("field", error.getField());
        attributes.put("rejectedValue", error.getRejectedValue());
        attributes.put("message", error.getDefaultMessage());
        return attributes;
    }
}
