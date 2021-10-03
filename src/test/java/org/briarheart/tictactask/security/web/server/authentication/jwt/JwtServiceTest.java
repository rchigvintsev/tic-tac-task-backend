package org.briarheart.tictactask.security.web.server.authentication.jwt;

import org.briarheart.tictactask.model.User;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.AccessToken;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.InvalidAccessTokenException;
import org.briarheart.tictactask.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
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
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Chigvintsev
 */
@ExtendWith(SpringExtension.class)
class JwtServiceTest {
    private static final String VALID_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJiaXRAbWFpbC5j"
            + "b20iLCJpYXQiOjE1NzA5NjkyMzIsImV4cCI6OTIxNDE1OTQ0OTk0NTAzMn0.GM-rTmJzgHEkOAYemY591p2DaB4Z5ueb1xiB8fq8FJ"
            + "2zxDUvKcHOHKIJhAbQdl3wsipT2NDYyDE6eDnvIWz0vw";
    private static final String EMPTY_CLAIMS_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.e30.MtKmKZuyC8pbmLny-pEbCdB4d"
            + "zSELEZZ4-ALnZxnGtjmIZPzkkqWwbDkadOSi04j288oTkKQF2uU7RgpAYdMng";
    private static final String EXPIRED_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJiaXRAbWFpbC"
            + "5jb20iLCJpYXQiOjE1NzA5NzA5ODQsImV4cCI6MTU3MDk3MDk4NX0.0xyLLqtl74jHlxWWFF7SqRzc9zJVHcm5HMJKcRwdMGOFN1Is"
            + "ie3Py-LrGk9pgQJnLTftv8cKv4hCBjrzX7eLjA";
    private static final String UNSUPPORTED_ACCESS_TOKEN_VALUE = "eyJhbGciOiJQUzM4NCIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ3aG"
            + "l0ZS5yYWJiaXRAbWFpbC5jb20iLCJpYXQiOjE1MTYyMzkwMjJ9.CQxfeQKAPPv28DtvTr5fz00xFV1RHpATQ5oThTi9Dh5fjXZvIHs"
            + "MM-ZYzNirTqvzxpAo8LNG9G5fS_Jsl2IQWFHXTIdV5C2TCgquW0ll3TDdajU_vy7sVILpY3d3NV5ngPcUaO7yaV4xFSWC25FwO0KHR"
            + "YcAR-pA-jAt1VIfLbj0VCN2OSCNN6loCzv49PwUwgbLJsHUqq_dGNz_M1xXIvPtGNlZ1EbSqT1Ya3-qPffGLESWi-gO5yvyOeWgv9Y"
            + "jkrnFAiHMCvrAxR64HLJqRLAJasIpU2BSFf0bpifdHAH95arDeWpxmYQXXDsZQ3MO6I3JWZvdJO7XPBv9fGrdMQ";
    private static final String MALFORMED_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJiaXRAbWFp"
            + "bC5jb20iLCJpYXQiOjE1NzA5NjkyMzIsImV4cCI6OTIxNDE1OTQ0OTk0NTAzMn0.GM-rTmJzgHEkOAYemY591p2DaB4Z5ueb1xiB8f"
            + "q8FJ2zxDUvKcHOHKIJhAbQdl3wsipT2NDYyDE6eDnvIWz0vw.";
    private static final String INVALID_SIGNATURE_ACCESS_TOKEN_VALUE = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ3aGl0ZS5yYWJi"
            + "aXRAbWFpbC5jb20iLCJpYXQiOjE1NzA5NzQxMjUsImV4cCI6OTIxNDE1OTQ0OTk0OTkyNX0.c3lywXl1i1hv3eg_jIgXaoGXqziLUt"
            + "gk7QIxb_Ty9z8stQZQJa50h1zbzLfA-7ZtQ30FFOb_v8BFvnQgZZDUSA";

    private static final String USER_EMAIL = "white.rabbit@mail.com";

    @Value("${application.security.authentication.access-token.signing-key}")
    private String accessTokenSigningKey;

    private JwtService service;

    @BeforeEach
    void setUp() {
        ServerAccessTokenRepository accessTokenRepositoryMock = mock(ServerAccessTokenRepository.class);
        when(accessTokenRepositoryMock.saveAccessToken(any(), any()))
                .thenAnswer(args -> Mono.just(args.getArgument(0, AccessToken.class)));

        service = new JwtService(accessTokenRepositoryMock, accessTokenSigningKey);
        service.setAccessTokenValiditySeconds(Long.MAX_VALUE / 1001);
    }

    @Test
    void shouldCreateAccessToken() {
        User user = User.builder().id(1L).email(USER_EMAIL).emailConfirmed(true).enabled(true).build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        AccessToken accessToken = service.createAccessToken(user, webExchangeMock).block();
        assertNotNull(accessToken);
        assertEquals(Long.toString(user.getId()), accessToken.getSubject());
    }

    @Test
    void shouldIncludeUserEmailInToken() {
        User user = User.builder().id(1L).email(USER_EMAIL).build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        AccessToken accessToken = service.createAccessToken(user, webExchangeMock).block();
        assertNotNull(accessToken);
        assertEquals(USER_EMAIL, accessToken.getEmail());
    }

    @Test
    void shouldIncludeUserFullNameInToken() {
        final String FULL_NAME = "White Rabbit";
        User user = User.builder().id(1L).fullName(FULL_NAME).build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        AccessToken accessToken = service.createAccessToken(user, webExchangeMock).block();
        assertNotNull(accessToken);
        assertEquals(FULL_NAME, accessToken.getFullName());
    }

    @Test
    void shouldIncludeUserProfilePictureUrlInToken() {
        final String PROFILE_PICTURE_URL = "https://example.com/picture";
        User user = User.builder().id(1L).profilePictureUrl(PROFILE_PICTURE_URL).build();

        MockServerHttpRequest requestMock = MockServerHttpRequest.get("/").build();
        MockServerWebExchange webExchangeMock = MockServerWebExchange.from(requestMock);

        AccessToken accessToken = service.createAccessToken(user, webExchangeMock).block();
        assertNotNull(accessToken);
        assertEquals(PROFILE_PICTURE_URL, accessToken.getProfilePictureUrl());
    }

    @Test
    void shouldParseAccessToken() {
        AccessToken accessToken = service.parseAccessToken(VALID_ACCESS_TOKEN_VALUE).block();
        assertNotNull(accessToken);
        assertEquals(USER_EMAIL, accessToken.getSubject());
    }

    @Test
    void shouldParseAccessTokenWithEmptyClaims() {
        AccessToken accessToken = service.parseAccessToken(EMPTY_CLAIMS_ACCESS_TOKEN_VALUE).block();
        assertNotNull(accessToken);
        assertNull(accessToken.getSubject());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsExpired() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(EXPIRED_ACCESS_TOKEN_VALUE).block());
        assertEquals("Access token is expired", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsUnsupported() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(UNSUPPORTED_ACCESS_TOKEN_VALUE).block());
        assertEquals("Access token is unsupported", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenAccessTokenIsMalformed() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(MALFORMED_ACCESS_TOKEN_VALUE).block());
        assertEquals("Access token is malformed", e.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenSignatureIsInvalid() {
        InvalidAccessTokenException e = assertThrows(InvalidAccessTokenException.class, () ->
                service.parseAccessToken(INVALID_SIGNATURE_ACCESS_TOKEN_VALUE).block());
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
