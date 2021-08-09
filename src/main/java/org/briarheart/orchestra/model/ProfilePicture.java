package org.briarheart.orchestra.model;

import lombok.*;
import org.springframework.data.annotation.Id;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProfilePicture {
    @Id
    private Long userId;
    private byte[] data;
    private String type;
}
