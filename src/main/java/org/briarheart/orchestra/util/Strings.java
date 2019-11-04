package org.briarheart.orchestra.util;

import org.springframework.util.StringUtils;

/**
 * Utility class providing various methods that make it easier to work with strings.
 *
 * @author Roman Chigvintsev
 */
public class Strings {
    private Strings() {
        //no instance
    }

    /**
     * Checks whether all of the given strings contain actual <em>text</em>.
     * <p>
     * More specifically, this method returns {@code true} if strings are not {@code null}, their lengths are greater
     * than 0 and they contain at least one non-whitespace character.
     * <p>
     * This method returns {@code false} when empty array is passed as an argument.
     *
     * @param strings strings to check
     * @return {@code true} if all of the given strings contain actual text, {@code false} otherwise
     */
    public static boolean hasText(String... strings) {
        if (strings != null && strings.length > 0) {
            for (String s : strings) {
                if (!StringUtils.hasText(s)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
