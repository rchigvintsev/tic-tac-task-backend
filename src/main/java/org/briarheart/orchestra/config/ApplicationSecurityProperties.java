package org.briarheart.orchestra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Roman Chigvintsev
 */
@Configuration
@ConfigurationProperties("application.security")
@Getter
@Setter
public class ApplicationSecurityProperties {
    private Authentication authentication;

    @Getter
    @Setter
    public static class Authentication {
        private AccessToken accessToken;
    }

    @Getter
    @Setter
    public static class AccessToken {
        private String signingKey;
        private long validitySeconds;
    }
}
