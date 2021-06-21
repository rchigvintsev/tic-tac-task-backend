package org.briarheart.orchestra.controller;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.orchestra.service.ImageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/v1/images")
public class ImageController extends AbstractController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        Assert.notNull(imageService, "Image service must not be null");
        this.imageService = imageService;
    }

    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable("id") Long id, Authentication authentication) {
        return imageService.getImage(id, getUser(authentication)).map(image -> ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(image.getImageData().length)
                .body(new ByteArrayResource(image.getImageData())));
    }
}
