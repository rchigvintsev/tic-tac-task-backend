package org.briarheart.orchestra.controller;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * POJO being used as a response body to inform client about details of some error.
 *
 * @author Roman Chigvintsev
 */
public class ErrorResponse {
    @Getter
    @Setter
    private String message;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorResponse)) return false;
        ErrorResponse response = (ErrorResponse) o;
        return Objects.equals(message, response.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }
}
