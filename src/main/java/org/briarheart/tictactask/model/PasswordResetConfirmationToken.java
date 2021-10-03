package org.briarheart.tictactask.model;

import lombok.*;

/**
 * Token that is generated to confirm reset of user's password.
 *
 * @author Roman Chigvintsev
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordResetConfirmationToken extends AbstractToken {
    @Getter
    @Setter
    private boolean valid = true;

    public PasswordResetConfirmationToken(PasswordResetConfirmationToken other) {
        super(other);
        this.valid = other.valid;
    }

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
