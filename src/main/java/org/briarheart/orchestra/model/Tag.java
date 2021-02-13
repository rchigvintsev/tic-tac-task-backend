package org.briarheart.orchestra.model;

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
     * Creates copy of this tag including all attributes except primary key.
     *
     * @return copy of this tag
     */
    public Tag copy() {
        Tag copy = new Tag();
        copy.setUserId(userId);
        copy.setName(name);
        copy.setColor(color);
        return copy;
    }
}
