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
     * Sends password reset link to the given user.
     *
     * @param user   user to which password reset link should be sent (must not be {@code null})
     * @param locale current user's locale
     * @throws UnableToSendMessageException if error occurred while trying to send password reset link
     */
    Mono<PasswordResetConfirmationToken> sendPasswordResetLink(User user, Locale locale) throws UnableToSendMessageException;

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
