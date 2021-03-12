package org.briarheart.orchestra.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Token that is generated to reset user's password.
 *
 * @author Roman Chigvintsev
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordResetToken extends AbstractToken {
    private PasswordResetToken(PasswordResetTokenBuilder builder) {
        super(builder);
    }

    public static PasswordResetTokenBuilder builder() {
        return new PasswordResetTokenBuilder();
    }

    public static class PasswordResetTokenBuilder extends AbstractTokenBuilder<PasswordResetToken> {
        private PasswordResetTokenBuilder() {
            //no instance
        }

        @Override
        public PasswordResetToken build() {
            return new PasswordResetToken(this);
        }
    }
}
