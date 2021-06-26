package org.briarheart.orchestra.service;

import org.briarheart.orchestra.data.EntityNotFoundException;
import org.briarheart.orchestra.data.ImageRepository;
import org.briarheart.orchestra.model.Image;
import org.briarheart.orchestra.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
class DefaultImageServiceTest {
    private ImageRepository imageRepository;
    private ImageService service;

    @BeforeEach
    void setUp() {
        imageRepository = mock(ImageRepository.class);
        service = new DefaultImageService(imageRepository);
    }

    @Test
    void shouldThrowExceptionOnConstructWhenImageRepositoryIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new DefaultImageService(null));
        assertEquals("Image repository must not be null", e.getMessage());
    }

    @Test
    void shouldReturnImageById() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        Image image = Image.builder().id(2L).userId(user.getId()).build();
        when(imageRepository.findByIdAndUserId(image.getId(), user.getId())).thenReturn(Mono.just(image));

        Image result = service.getImage(image.getId(), user).block();
        assertEquals(image, result);
    }

    @Test
    void shouldThrowExceptionOnImageGetWhenUserIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.getImage(1L, null).block());
        assertEquals("User must not be null", e.getMessage());
    }

    @Test
    void shouldThrowExceptionOnImageGetWhenImageIsNotFound() {
        User user = User.builder().id(1L).email("alice@mail.com").build();
        when(imageRepository.findByIdAndUserId(anyLong(), anyLong())).thenReturn(Mono.empty());
        long imageId = 2L;
        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> service.getImage(imageId, user).block());
        assertEquals("Image with id " + imageId + " is not found", e.getMessage());
    }

    @Test
    void shouldCreateImage() {
        long imageId = 1L;
        when(imageRepository.save(any(Image.class))).thenAnswer(args -> {
            Image i = args.getArgument(0);
            if (i.getId() == null) {
                i.setId(imageId);
            }
            return Mono.just(i);
        });

        Image image = new Image();

        Image expectedResult = new Image(image);
        expectedResult.setId(imageId);

        Image result = service.createImage(image).block();
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldThrowExceptionOnImageCreateWhenImageIsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.createImage(null));
        assertEquals("Image must not be null", e.getMessage());
    }
}
