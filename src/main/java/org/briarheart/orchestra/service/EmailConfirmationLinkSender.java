package org.briarheart.orchestra.service;

import org.briarheart.orchestra.model.EmailConfirmationToken;
import org.briarheart.orchestra.model.User;
import reactor.core.publisher.Mono;

/**
 * Component that is used to send email address confirmation link to user to complete his/her registration.
 *
 * @author Roman Chigvintsev
 */
public interface EmailConfirmationLinkSender {
    /**
     * Sends email confirmation link to the given user.
     *
     * @param user user to which email confirmation link should be sent (must not be {@code null})
     * @param token email confirmation token that will be used to activate user's account (must not be {@code null})
     */
    Mono<Void> sendEmailConfirmationLink(User user, EmailConfirmationToken token);
}
