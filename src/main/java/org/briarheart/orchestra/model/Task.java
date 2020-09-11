package org.briarheart.orchestra.model;

import lombok.*;
import org.briarheart.orchestra.model.validation.constraints.NotPast;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
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
@NotPast(dateFieldName = "deadlineDate", timeFieldName = "deadlineTime")
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
    private List<Tag> tags = Collections.emptyList();

    private String author;
    private LocalDate deadlineDate;
    private LocalTime deadlineTime;

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
        copy.setDeadlineDate(deadlineDate);
        copy.setDeadlineTime(deadlineTime);
        copy.setTags(this.tags.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.tags));
        return copy;
    }
}
