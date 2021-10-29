package org.briarheart.tictactask.user.authority;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.briarheart.tictactask.user.User;

/**
 * Many-to-many relation between {@link User} and authority.
 *
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthorityRelation {
    private Long userId;
    private String authority;
}
