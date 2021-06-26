package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.model.Image;
import org.briarheart.orchestra.model.User;
import reactor.core.publisher.Mono;

/**
 * Service for managing of images.
 *
 * @author Roman Chigvintsev
 */
public interface ImageService {
    /**
     * Returns image with the given id and belonging to the given user.
     *
     * @param id image id
     * @param user image owner (must not be {@code null})
     * @return requested image
     * @throws EntityNotFoundException if image is not found by id or does not belong to the given user
     */
    Mono<Image> getImage(Long id, User user) throws EntityNotFoundException;

    /**
     * Creates new image.
     *
     * @param image image to be created (must not be {@code null})
     * @return created image
     */
    Mono<Image> createImage(Image image);
}
