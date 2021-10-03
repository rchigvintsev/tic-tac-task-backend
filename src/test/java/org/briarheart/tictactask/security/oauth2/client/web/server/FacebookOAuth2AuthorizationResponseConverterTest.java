package org.briarheart.tictactask.security.oauth2.client.web.server;

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
class FacebookOAuth2AuthorizationResponseConverterTest {
    private FacebookOAuth2AuthorizationResponseConverter converter;

    @BeforeEach
    void setUp() {
        converter = new FacebookOAuth2AuthorizationResponseConverter();
    }

    @Test
    void shouldConvertToErrorResponseWhenAuthorizationCodeIsNotProvided() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.STATE, "abc");
        params.add("error_code", "1024");
        params.add("error_message", "Something went wrong");
        OAuth2AuthorizationResponse response = converter.convert(params, "https://client.com/authorization/callback");
        assertNotNull(response);
        assertTrue(response.statusError());
    }

    @Test
    void shouldUseErrorMessageAsErrorDescriptionOnConvertWhenAuthorizationCodeIsNotProvided() {
        String errorMessage = "Something went wrong";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add(OAuth2ParameterNames.STATE, "abc");
        params.add("error_code", "1024");
        params.add("error_message", errorMessage);
        OAuth2AuthorizationResponse response = converter.convert(params, "https://client.com/authorization/callback");
        assertNotNull(response);
        assertEquals(errorMessage, response.getError().getDescription());
    }
}
