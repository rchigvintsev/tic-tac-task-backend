package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.ProfilePicture;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Roman Chigvintsev
 */
public interface ProfilePictureRepository extends ReactiveCrudRepository<ProfilePicture, Long>, ProfilePictureCreator {
}
