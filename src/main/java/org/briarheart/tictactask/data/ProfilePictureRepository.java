package org.briarheart.tictactask.data;

import org.briarheart.tictactask.model.ProfilePicture;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Roman Chigvintsev
 */
public interface ProfilePictureRepository extends ReactiveCrudRepository<ProfilePicture, Long>, ProfilePictureCreator {
}
