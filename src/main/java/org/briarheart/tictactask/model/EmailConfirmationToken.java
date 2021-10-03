package org.briarheart.tictactask.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Token that is generated on user registration to confirm user's email address.
 *
 * @author Roman Chigvintsev
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmailConfirmationToken extends AbstractToken {
    private EmailConfirmationToken(EmailConfirmationTokenBuilder builder) {
        super(builder);
    }

    public static EmailConfirmationTokenBuilder builder() {
        return new EmailConfirmationTokenBuilder();
    }

    public static class EmailConfirmationTokenBuilder extends AbstractTokenBuilder<EmailConfirmationToken> {
        private EmailConfirmationTokenBuilder() {
            //no instance
        }

        @Override
        public EmailConfirmationToken build() {
            return new EmailConfirmationToken(this);
        }
    }
}
