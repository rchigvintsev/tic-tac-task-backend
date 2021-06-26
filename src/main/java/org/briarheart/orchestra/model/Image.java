package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.util.Arrays;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Image {
    @Id
    private Long id;
    private Long userId;
    private byte[] data;
    private String type;

    /**
     * Creates copy of the given image.
     *
     * @param other image to be copied (must not be {@code null})
     */
    public Image(Image other) {
        Assert.notNull(other, "Image must not be null");
        this.id = other.id;
        this.userId = other.userId;
        this.data = other.data != null ? Arrays.copyOf(other.data, other.data.length) : null;
        this.type = other.type;
    }
}
