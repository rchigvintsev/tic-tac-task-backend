package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import javax.validation.Valid;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 10_000)
    private String description;

    @Builder.Default
    private TaskStatus status = TaskStatus.UNPROCESSED;

    @Builder.Default
    @Transient
    @Valid
    private List<Tag> tags = Collections.emptyList();

    private String author;

    @FutureOrPresent
    private LocalDateTime deadline;

    private boolean deadlineTimeExplicitlySet;

    /**
     * Creates copy of this task including all attributes except primary key.
     *
     * @return copy of this task
     */
    public Task copy() {
        Task copy = new Task();
        copy.setTitle(title);
        copy.setDescription(description);
        copy.setStatus(status);
        copy.setAuthor(author);
        copy.setDeadline(deadline);
        copy.setDeadlineTimeExplicitlySet(deadlineTimeExplicitlySet);
        copy.setTags(this.tags.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.tags));
        return copy;
    }
}
