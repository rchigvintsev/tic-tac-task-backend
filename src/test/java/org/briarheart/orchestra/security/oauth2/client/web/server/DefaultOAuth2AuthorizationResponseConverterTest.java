package org.briarheart.orchestra.security.oauth2.client.web.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Chigvintsev
 */
class DefaultOAuth2AuthorizationResponseConverterTest {
    private DefaultOAuth2AuthorizationResponseConverter converter;

    @BeforeEach
    void setUp() {
        converter = new DefaultOAuth2AuthorizationResponseConverter();
    }

    @Test
    void shouldConvertToSuccessResponse() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.CODE, "12345");
        params.add(OAuth2ParameterNames.STATE, "abc");
        OAuth2AuthorizationResponse response = converter.convert(params, "https://client.com/authorization/callback");
        assertNotNull(response);
        assertTrue(response.statusOk());
    }

    @Test
    void shouldConvertToErrorResponseWhenAuthorizationCodeIsNotProvided() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.STATE, "abc");
        params.add(OAuth2ParameterNames.ERROR, "1024");
        params.add(OAuth2ParameterNames.ERROR_DESCRIPTION, "Something went wrong");
        OAuth2AuthorizationResponse response = converter.convert(params, "https://client.com/authorization/callback");
        assertNotNull(response);
        assertTrue(response.statusError());
    }

    @Test
    void shouldThrowExceptionOnConvertWhenRequestParametersAreNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(null, "https://client.com/authorization/callback"));
        assertEquals("Request parameters must not be null", e.getMessage());
    }
}
