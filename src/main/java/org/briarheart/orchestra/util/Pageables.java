package org.briarheart.orchestra.util;

import org.springframework.data.domain.Pageable;

/**
 * Utility class that provides handy methods to get 'OFFSET' and 'LIMIT' values from {@link Pageable}.
 *
 * @author Roman Chigvintsev
 */
public class Pageables {
    private Pageables() {
        //no instance
    }

    /**
     * Returns offset from the given {@link Pageable} or zero if {@link Pageable} is {@code null} or not paged.
     *
     * @param pageable instance of {@link Pageable} to get offset
     * @return offset
     */
    public static long getOffset(Pageable pageable) {
        return pageable != null && pageable.isPaged() ? pageable.getOffset() : 0L;
    }

    /**
     * Returns limit from the given {@link Pageable} or {@code null} if {@link Pageable} is {@code null} or not paged.
     *
     * @param pageable instance of {@link Pageable} to get limit
     * @return limit or {@code null} if given {@link Pageable} is {@code null} or not paged
     */
    public static Integer getLimit(Pageable pageable) {
        return pageable != null && pageable.isPaged() ? pageable.getPageSize() : null;
    }
}
