package org.briarheart.tictactask.email;

/**
 * Service to send simple emails on behalf of application.
 */
public interface EmailService {
    /**
     * Sends simple email to the given user.
     *
     * @param to user email address (must not be {@code null} or empty)
     * @param subject email subject (must not be {@code null} or empty)
     * @param text email text
     */
    void sendEmail(String to, String subject, String text);
}
