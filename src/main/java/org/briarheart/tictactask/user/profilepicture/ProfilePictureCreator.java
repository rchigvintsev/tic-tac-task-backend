package org.briarheart.tictactask.user.profilepicture;

import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface ProfilePictureCreator {
    Mono<ProfilePicture> create(ProfilePicture picture);
}
