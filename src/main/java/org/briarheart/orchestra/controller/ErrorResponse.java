package org.briarheart.orchestra.controller;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO being used as a response body to inform client about details of some error.
 *
 * @author Roman Chigvintsev
 */
@EqualsAndHashCode
public class ErrorResponse {
    @Getter
    @Setter
    private String message;
}
