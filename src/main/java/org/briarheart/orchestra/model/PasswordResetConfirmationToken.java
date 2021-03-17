package org.briarheart.orchestra.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Token that is generated to confirm reset of user's password.
 *
 * @author Roman Chigvintsev
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordResetConfirmationToken extends AbstractToken {
    private PasswordResetConfirmationToken(PasswordResetConfirmationTokenBuilder builder) {
        super(builder);
    }

    public static PasswordResetConfirmationTokenBuilder builder() {
        return new PasswordResetConfirmationTokenBuilder();
    }

    public static class PasswordResetConfirmationTokenBuilder extends AbstractTokenBuilder<PasswordResetConfirmationToken> {
        private PasswordResetConfirmationTokenBuilder() {
            //no instance
        }

        @Override
        public PasswordResetConfirmationToken build() {
            return new PasswordResetConfirmationToken(this);
        }
    }
}
