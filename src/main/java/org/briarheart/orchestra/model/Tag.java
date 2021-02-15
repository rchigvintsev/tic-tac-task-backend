package org.briarheart.orchestra.model;

import io.jsonwebtoken.lang.Assert;
import lombok.*;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Tag {
    @Id
    private Long id;
    private Long userId;
    @NotBlank
    @Size(max = 50)
    private String name;
    private Integer color;

    /**
     * Creates copy of the given tag.
     *
     * @param tag tag to be copied (must not be {@code null})
     */
    public Tag(Tag tag) {
        Assert.notNull(tag, "Tag must not be null");
        this.id = tag.id;
        this.userId = tag.userId;
        this.name = tag.name;
        this.color = tag.color;
    }
}
