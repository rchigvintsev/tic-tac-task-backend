package org.briarheart.tictactask.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Configuration
@ConfigurationProperties("application")
@Getter
@Setter
public class ApplicationProperties {
    private String name;
    private String version;
    private String domain;
    private Security security = new Security();

    @Getter
    @Setter
    public static class Security {
        private Cors cors = new Cors();
        private Authentication authentication = new Authentication();
        private PasswordReset passwordReset = new PasswordReset();
        private EmailConfirmation emailConfirmation = new EmailConfirmation();

        @Getter
        @Setter
        public static class Cors {
            private String allowedOrigin = "http://localhost:4200";
        }

        @Getter
        @Setter
        public static class Authentication {
            private AccessToken accessToken = new AccessToken();

            @Getter
            @Setter
            public static class AccessToken {
                private String signingKey;
                private long validitySeconds = 60_4800L; // Seven days
            }
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
}
