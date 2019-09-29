package org.briarheart.orchestra.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Roman Chigvintsev
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    public static final User EMPTY = new ImmutableUser();

    @Id
    private String email;

    @Version
    private long version;

    private String fullName;
    private String imageUrl;

    private static class ImmutableUser extends User {
        @Override
        public void setEmail(String email) {
            // Do nothing
        }

        @Override
        public void setVersion(long version) {
            // Do nothing
        }

        @Override
        public void setFullName(String name) {
            // Do nothing
        }

        @Override
        public void setImageUrl(String imageUrl) {
            // Do nothing
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
