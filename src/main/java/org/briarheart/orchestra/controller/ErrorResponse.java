package org.briarheart.orchestra.controller;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * POJO being used as a response body to inform client about details of some errors.
 *
 * @author Roman Chigvintsev
 */
@EqualsAndHashCode
public class ErrorResponse {
    @Getter
    @Setter
    private List<String> errors;
}
