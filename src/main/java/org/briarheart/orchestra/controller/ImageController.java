package org.briarheart.orchestra.controller;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.orchestra.model.Image;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.service.ImageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.net.URI;

/**
 * @author Roman Chigvintsev
 */
@RestController
@RequestMapping("/v1/images")
public class ImageController extends AbstractController {
    private final ImageService imageService;

    /**
     * Creates new instance of this class with the given image service.
     *
     * @param imageService image service (must not be {@code null})
     */
    public ImageController(ImageService imageService) {
        Assert.notNull(imageService, "Image service must not be null");
        this.imageService = imageService;
    }

    @GetMapping(path = "/{id}")
    public Mono<ResponseEntity<Resource>> getImage(@PathVariable("id") Long id, Authentication authentication) {
        return imageService.getImage(id, getUser(authentication)).map(image -> {
            ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
            if (image.getType() != null) {
                bodyBuilder.contentType(MediaType.parseMediaType(image.getType()));
            }
            return bodyBuilder.contentLength(image.getData().length).body(new ByteArrayResource(image.getData()));
        });
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> createImage(@RequestPart("image") Mono<FilePart> imageFile,
                                                    Authentication authentication,
                                                    ServerHttpRequest request) {
        User user = getUser(authentication);
        return imageFile.flatMapMany(Part::content)
                .reduce(new ByteArrayOutputStream(), (buffer, content) -> {
                    buffer.writeBytes(content.asByteBuffer().array());
                    DataBufferUtils.release(content);
                    return buffer;
                }).zipWith(imageFile.map(Part::headers))
                .flatMap(contentAndHeaders -> {
                    byte[] imageData = contentAndHeaders.getT1().toByteArray();
                    HttpHeaders headers = contentAndHeaders.getT2();
                    MediaType contentType = headers.getContentType();
                    String imageType = contentType != null ? contentType.toString() : null;
                    Image image = Image.builder().userId(user.getId()).data(imageData).type(imageType).build();
                    return imageService.createImage(image);
                }).map(image -> {
                    URI taskLocation = UriComponentsBuilder.fromHttpRequest(request)
                            .path("/{id}")
                            .buildAndExpand(image.getId())
                            .toUri();
                    return ResponseEntity.created(taskLocation).build();
                });
    }
}
