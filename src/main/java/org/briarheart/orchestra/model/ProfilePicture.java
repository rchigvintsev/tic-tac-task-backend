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
public class ProfilePicture {
    @Id
    private Long userId;
    private byte[] data;
    private String type;

    /**
     * Creates copy of the given profile picture.
     *
     * @param other profile picture to be copied (must not be {@code null})
     */
    public ProfilePicture(ProfilePicture other) {
        Assert.notNull(other, "User profile picture must not be null");
        this.userId = other.userId;
        this.data = other.data != null ? Arrays.copyOf(other.data, other.data.length) : null;
        this.type = other.type;
    }
}
