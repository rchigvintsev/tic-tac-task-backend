package org.briarheart.tictactask.data;

import org.briarheart.tictactask.model.ProfilePicture;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface ProfilePictureCreator {
    Mono<ProfilePicture> create(ProfilePicture picture);
}
