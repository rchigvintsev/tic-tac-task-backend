package org.briarheart.tictactask.util;

/**
 * Utility class that provides JWTs for integration tests.
 *
 * @author Roman Chigvintsev
 */
public class TestAccessTokens {
    private TestAccessTokens() {
        //no instance
    }

    public static final String JOHN_DOE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJqb2huLmRvZUBtYWlsLmNvbSIsIm5"
            + "hbWUiOiJKb2huIERvZSIsImlhdCI6MTYwNjIxNjk5NCwiZXhwIjozMzE0MjIxNjk5NH0.S5KuV3QN0klawq5CAh7tDcrlW0oPaxLNlyo"
            + "J8ScqrO3uKImdlB8Wdv0dRPIt7TAxy1mXPNJsrczUqLr4IDKVdw";
}
