package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.PasswordResetConfirmationToken;
import org.briarheart.orchestra.model.User;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Service for managing of user's passwords.
 *
 * @author Roman Chigvintsev
 */
public interface PasswordService {
    /**
     * Sends password reset link to the given email address provided user with such email address exists and enabled.
     * Does nothing otherwise.
     *
     * @param email email address of user whose password should be reset (must not be {@code null} or empty)
     * @param locale current user's locale
     */
    Mono<Void> resetPassword(String email, Locale locale);

    /**
     * Resets user's password provided the given confirmation token is valid.
     *
     * @param userId      id of user whose password must be reset
     * @param token       password reset confirmation token
     * @param newPassword new password
     * @throws EntityNotFoundException if user is not found by id or given password reset confirmation token is not
     *                                 registered for user
     * @throws TokenExpiredException   if the given password reset confirmation token is expired
     */
    Mono<Void> confirmPasswordReset(Long userId, String token, String newPassword)
            throws EntityNotFoundException, TokenExpiredException;
}
