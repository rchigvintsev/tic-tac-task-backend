package org.briarheart.orchestra.model;

import lombok.*;
import org.briarheart.orchestra.model.validation.constraints.NotPast;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

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
        return copy;
    }
}
