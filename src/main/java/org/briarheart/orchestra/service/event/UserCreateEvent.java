package org.briarheart.orchestra.service.event;

import org.briarheart.orchestra.model.User;
import org.springframework.context.ApplicationEvent;

/**
 * @author Roman Chigvintsev
 */
public class UserCreateEvent extends ApplicationEvent implements ServiceEvent {
    /**
     * Create a new {@code ApplicationEvent}.
     *
     * @param source the object on which the event initially occurred or with
     *               which the event is associated (never {@code null})
     */
    public UserCreateEvent(User user) {
        super(user);
    }

    public User getUser() {
        return (User) getSource();
    }
}
