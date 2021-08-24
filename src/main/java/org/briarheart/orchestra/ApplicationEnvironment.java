package org.briarheart.orchestra;

import org.springframework.util.StringUtils;

/**
 * Utility class that provides methods to get values of various application specific environment variables.
 */
public class ApplicationEnvironment {
    private ApplicationEnvironment() {
        //no instance
    }

    /**
     * Returns value of "SECURITY_BASE_REDIRECT_URI" environment variable representing base URI to perform client
     * redirect. Usually it is a base URI of frontend application. If "SECURITY_BASE_REDIRECT_URI" variable is not
     * provided this method uses "http://localhost:4200" as default redirect URI.
     *
     * @return base client redirect URI
     */
    public static String getBaseRedirectUri() {
        String baseRedirectUri = System.getenv("SECURITY_BASE_REDIRECT_URI");
        if (!StringUtils.hasLength(baseRedirectUri)) {
            baseRedirectUri = "http://localhost:4200";
        }
        return baseRedirectUri;
    }
}
