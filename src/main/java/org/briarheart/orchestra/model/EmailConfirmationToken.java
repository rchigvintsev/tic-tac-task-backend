package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Token that is generated on user registration to confirm user's email address.
 *
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class EmailConfirmationToken {
    @Id
    private Long id;

    private String email;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return !expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC));
    }
}
