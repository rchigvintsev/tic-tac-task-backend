package org.briarheart.tictactask.util;

import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.RequestPredicate;

/**
 * Provides {@link RequestPredicate}s in addition to those provided by
 * {@link org.springframework.web.reactive.function.server.RequestPredicates}.
 */
public class RequestPredicates {
    private RequestPredicates() {
        //no instance
    }

    /**
     * Returns {@link RequestPredicate} that matches if request path contains (in case-insensitive manner)
     * the given value.
     *
     * @param pathPart path part (must not be {@code null})
     * @return instance of {@link RequestPredicate}
     */
    public static RequestPredicate pathContains(String pathPart) {
        Assert.notNull(pathPart, "Path part must not be null");
        return request -> request.path().toLowerCase().contains(pathPart);
    }
}
