package org.briarheart.tictactask.util;

import org.briarheart.tictactask.model.User;

/**
 * Utility class that provides users for integration tests.
 *
 * @author Roman Chigvintsev
 */
public class TestUsers {
    private TestUsers() {
        //no instance
    }

    public static final User JOHN_DOE = User.builder()
            .id(1L)
            .email("john.doe@mail.com")
            .emailConfirmed(true)
            .fullName("John Doe")
            .enabled(true)
            .build();

    public static final User JANE_DOE = User.builder()
            .id(2L)
            .email("jane.doe@mail.com")
            .emailConfirmed(false)
            .fullName("Jane Doe")
            .enabled(false)
            .build();
}
