package org.briarheart.orchestra.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Main entity representing task to be done.
 */
@Data
@NoArgsConstructor
public class Task {
    @Id
    private Long id;

    @NotBlank(message = "{javax.validation.constraints.NotBlank.title.message}")
    @Size(max = 255, message = "{javax.validation.constraints.Size.title.message}")
    private String title;

    @Size(max = 10_000, message = "{javax.validation.constraints.Size.description.message}")
    private String description;

    @NotNull(message = "{javax.validation.constraints.NotNull.completed.message}")
    private Boolean completed = false;
}
