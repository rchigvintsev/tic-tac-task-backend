package org.briarheart.tictactask.user.profilepicture;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * @author Roman Chigvintsev
 */
public interface ProfilePictureRepository
        extends ReactiveCrudRepository<ProfilePicture, Long>, CustomizedProfilePictureRepository {
}
