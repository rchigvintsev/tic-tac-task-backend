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

    @NotBlank
    @Size(max = 50)
    private String name;

    private String author;

    /**
     * Creates copy of this tag including all attributes except primary key.
     *
     * @return copy of this tag
     */
    public Tag copy() {
        Tag copy = new Tag();
        copy.setName(name);
        copy.setAuthor(author);
        return copy;
    }
}
