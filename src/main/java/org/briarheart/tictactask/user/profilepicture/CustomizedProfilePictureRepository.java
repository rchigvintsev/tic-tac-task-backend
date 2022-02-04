package org.briarheart.tictactask.user.profilepicture;

import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface CustomizedProfilePictureRepository {
    Mono<ProfilePicture> create(ProfilePicture picture);
}
