package org.briarheart.tictactask.service;

/**
 * Exception indicating that password is not valid.
 *
 * @author Roman Chigvintsev
 */
public class InvalidPasswordException extends RuntimeException {
    private final String password;

    /**
     * Creates new instance of this class with the given invalid password.
     *
     * @param password invalid password
     */
    public InvalidPasswordException(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
