package org.briarheart.orchestra.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Main entity representing task to be done.
 */
@Entity
@Data
@NoArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "{javax.validation.constraints.NotBlank.title.message}")
    @Size(max = 255, message = "{javax.validation.constraints.Size.title.message}")
    private String title;

    @Size(max = 10_000, message = "{javax.validation.constraints.Size.description.message}")
    private String description;

    @NotNull(message = "{javax.validation.constraints.NotNull.completed.message}")
    private Boolean completed = false;

    /**
     * Creates new instance of this class with the given title.
     *
     * @param title task title (must not be {@code null} or empty)
     */
    public Task(String title) {
        setTitle(title);
    }

    public void setTitle(String title) {
        Assert.hasText(title, "Task title must not be null or blank");
        this.title = title.trim();
    }
}
