package org.briarheart.orchestra.service;

import org.briarheart.orchestra.model.PasswordResetToken;
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
    Mono<PasswordResetToken> sendPasswordResetLink(User user, Locale locale) throws UnableToSendMessageException;
}
