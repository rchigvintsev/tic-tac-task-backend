package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * Service that is used to confirm user's email address to complete his/her registration.
 *
 * @author Roman Chigvintsev
 */
public interface EmailConfirmationService {
    /**
     * Sends email confirmation link to the given user.
     *
     * @param user   user to which email confirmation link should be sent (must not be {@code null})
     * @param locale current user's locale
     * @throws UnableToSendMessageException if error occurred while trying to send email confirmation link
     */
    Mono<EmailConfirmationToken> sendEmailConfirmationLink(User user, Locale locale)
            throws UnableToSendMessageException;

    /**
     * Confirms email for user with the given id.
     *
     * @param userId id of user whose email must be confirmed
     * @param token  email confirmation token
     * @throws EntityNotFoundException if user is not found by id or given email confirmation token is
     *                                 not registered for user
     * @throws TokenExpiredException   if the given email confirmation token is expired
     */
    Mono<Void> confirmEmail(Long userId, String token) throws EntityNotFoundException, TokenExpiredException;
}
