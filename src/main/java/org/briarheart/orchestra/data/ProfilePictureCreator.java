package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.ProfilePicture;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
public interface ProfilePictureCreator {
    Mono<ProfilePicture> create(ProfilePicture picture);
}
