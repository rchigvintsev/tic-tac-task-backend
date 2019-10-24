package org.briarheart.orchestra.security.web.server.authentication.jwt;

import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.web.server.authentication.AccessToken;
import org.briarheart.orchestra.security.web.server.authentication.InvalidAccessTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
class JwtServiceTest {
    private static final String VALID_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJiaXRAbWFpbC5j" +
            "b20iLCJpYXQiOjE1NzA5NjkyMzIsImV4cCI6OTIxNDE1OTQ0OTk0NTAzMn0.GM-rTmJzgHEkOAYemY591p2DaB4Z5ueb1xiB8fq8FJ2z" +
            "xDUvKcHOHKIJhAbQdl3wsipT2NDYyDE6eDnvIWz0vw";
    private static final String EMPTY_CLAIMS_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.e30.MtKmKZuyC8pbmLny-pEbCdB4d" +
            "zSELEZZ4-ALnZxnGtjmIZPzkkqWwbDkadOSi04j288oTkKQF2uU7RgpAYdMng";
    private static final String EXPIRED_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJiaXRAbWFpbC" +
            "5jb20iLCJpYXQiOjE1NzA5NzA5ODQsImV4cCI6MTU3MDk3MDk4NX0.0xyLLqtl74jHlxWWFF7SqRzc9zJVHcm5HMJKcRwdMGOFN1Isie" +
            "3Py-LrGk9pgQJnLTftv8cKv4hCBjrzX7eLjA";
    private static final String UNSUPPORTED_ACCESS_TOKEN_VALUE = "eyJhbGciOiJQUzM4NCIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ3aG" +
            "l0ZS5yYWJiaXRAbWFpbC5jb20iLCJpYXQiOjE1MTYyMzkwMjJ9.CQxfeQKAPPv28DtvTr5fz00xFV1RHpATQ5oThTi9Dh5fjXZvIHsMM" +
            "-ZYzNirTqvzxpAo8LNG9G5fS_Jsl2IQWFHXTIdV5C2TCgquW0ll3TDdajU_vy7sVILpY3d3NV5ngPcUaO7yaV4xFSWC25FwO0KHRYcAR" +
            "-pA-jAt1VIfLbj0VCN2OSCNN6loCzv49PwUwgbLJsHUqq_dGNz_M1xXIvPtGNlZ1EbSqT1Ya3-qPffGLESWi-gO5yvyOeWgv9YjkrnFA" +
            "iHMCvrAxR64HLJqRLAJasIpU2BSFf0bpifdHAH95arDeWpxmYQXXDsZQ3MO6I3JWZvdJO7XPBv9fGrdMQ";
    private static final String MALFORMED_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJiaXRAbWFp" +
            "bC5jb20iLCJpYXQiOjE1NzA5NjkyMzIsImV4cCI6OTIxNDE1OTQ0OTk0NTAzMn0.GM-rTmJzgHEkOAYemY591p2DaB4Z5ueb1xiB8fq8" +
            "FJ2zxDUvKcHOHKIJhAbQdl3wsipT2NDYyDE6eDnvIWz0vw.";
    private static final String INVALID_SIGNATURE_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJi" +
            "aXRAbWFpbC5jb20iLCJpYXQiOjE1NzA5NzQxMjUsImV4cCI6OTIxNDE1OTQ0OTk0OTkyNX0.c3lywXl1i1hv3eg_jIgXaoGXqziLUtgk" +
            "7QIxb_Ty9z8stQZQJa50h1zbzLfA-7ZtQ30FFOb_v8BFvnQgZZDUSA";

    private static final String USER_EMAIL = "white.rabbit@mail.com";

    @Value("${application.security.authentication.access-token.signing-key}")
    private String accessTokenSigningKey;

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(accessTokenSigningKey);
        service.setAccessTokenValiditySeconds(Long.MAX_VALUE / 1001);
    }

    @Test
    void shouldCreateAccessToken() {
        User user = new User();
        user.setEmail(USER_EMAIL);
        AccessToken accessToken = service.createAccessToken(user);
        assertNotNull(accessToken);
        assertEquals(USER_EMAIL, accessToken.getSubject());
    }

    @Test
    void shouldParseAccessToken() {
        AccessToken accessToken = service.parseAccessToken(VALID_ACCESS_TOKEN_VALUE);
        assertNotNull(accessToken);
        assertEquals(USER_EMAIL, accessToken.getSubject());
    }

    @Test
    void shouldParseAccessTokenWithEmptyClaims() {
        AccessToken accessToken = service.parseAccessToken(EMPTY_CLAIMS_ACCESS_TOKEN_VALUE);
        assertNotNull(accessToken);
        assertNull(accessToken.getSubject());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsExpired() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(EXPIRED_ACCESS_TOKEN_VALUE));
        assertEquals("Access token is expired", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsUnsupported() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(UNSUPPORTED_ACCESS_TOKEN_VALUE));
        assertEquals("Access token is unsupported", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsMalformed() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(MALFORMED_ACCESS_TOKEN_VALUE));
        assertEquals("Access token is malformed", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSignatureIsInvalid() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(INVALID_SIGNATURE_ACCESS_TOKEN_VALUE));
        assertEquals("Access token signature is not valid", e.getMessage());
    }

    @Configuration
    @PropertySource("classpath:application-test.yml")
    public static class JwtServiceTestConfig {
        @Bean
        public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
            yamlPropertiesFactoryBean.setResources(new ClassPathResource("application-test.yml"));

            PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
            Properties properties = yamlPropertiesFactoryBean.getObject();
            assert properties != null;
            pspc.setProperties(properties);
            return pspc;
        }
    }
}
