package org.briarheart.orchestra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@ConfigurationProperties("application.security")
@Getter
@Setter
public class ApplicationSecurityProperties {
    private Authentication authentication = new Authentication();
    private PasswordReset passwordReset = new PasswordReset();
    private EmailConfirmation emailConfirmation = new EmailConfirmation();

    @Getter
    @Setter
    public static class Authentication {
        private AccessToken accessToken = new AccessToken();
    }

    @Getter
    @Setter
    public static class AccessToken {
        private String signingKey;
        private long validitySeconds;
    }

    @Getter
    @Setter
    public static class PasswordReset {
        private Duration tokenExpirationTimeout = Duration.of(24, ChronoUnit.HOURS);

    }
    @Getter
    @Setter
    public static class EmailConfirmation {
        private Duration tokenExpirationTimeout = Duration.of(24, ChronoUnit.HOURS);
    }
}
