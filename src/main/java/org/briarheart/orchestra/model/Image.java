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
public class Image {
    @Id
    private Long id;
    private Long userId;
    private byte[] imageData;
}
