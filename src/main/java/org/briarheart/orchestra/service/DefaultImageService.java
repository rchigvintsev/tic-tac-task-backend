package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.ImageRepository;
import org.briarheart.orchestra.model.Image;
import org.briarheart.orchestra.model.User;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link ImageService}.
 *
 * @author Roman Chigvintsev
 */
@Service
public class DefaultImageService implements ImageService {
    private final ImageRepository imageRepository;

    public DefaultImageService(ImageRepository imageRepository) {
        Assert.notNull(imageRepository, "Image repository must not be null");
        this.imageRepository = imageRepository;
    }

    @Override
    public Mono<Image> getImage(Long id, User user) {
        Assert.notNull(user, "User must not be null");
        return imageRepository.findByIdAndUserId(id, user.getId())
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Image with id " + id + " is not found")));
    }
}
