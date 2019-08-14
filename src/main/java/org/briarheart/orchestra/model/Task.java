package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Main entity representing task to be done.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Task {
    @Id
    private Long id;

    @NotBlank(message = "{javax.validation.constraints.NotBlank.title.message}")
    @Size(max = 255, message = "{javax.validation.constraints.Size.title.message}")
    private String title;

    @Size(max = 10_000, message = "{javax.validation.constraints.Size.description.message}")
    private String description;

    @NotNull(message = "{javax.validation.constraints.NotNull.completed.message}")
    @Builder.Default
    private Boolean completed = false;
}
