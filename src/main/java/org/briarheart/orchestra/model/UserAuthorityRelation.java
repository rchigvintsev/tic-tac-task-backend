package org.briarheart.orchestra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Many-to-many relation between {@link User} and authority.
 *
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthorityRelation {
    private String email;
    private String authority;
}
