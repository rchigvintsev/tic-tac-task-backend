package org.briarheart.orchestra.controller;

import org.briarheart.orchestra.model.User;
import org.springframework.security.core.Authentication;

/**
 * Base class for all controllers.
 *
 * @author Roman Chigvintsev
 */
public abstract class AbstractController {
    protected User getUser(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
