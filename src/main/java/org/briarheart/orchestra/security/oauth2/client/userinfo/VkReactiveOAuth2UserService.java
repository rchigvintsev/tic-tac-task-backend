package org.briarheart.orchestra.security.oauth2.client.userinfo;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import lombok.NonNull;
import lombok.Setter;
import net.minidev.json.JSONObject;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;

/**
 * VK specific implementation of {@link ReactiveOAuth2UserService}.
 *
 * @author Roman Chigvintsev
 */
public class VkReactiveOAuth2UserService implements ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final String ERROR_CODE_INVALID_USER_INFO_RESPONSE = "invalid_user_info_response";
    private static final String ERROR_CODE_MISSING_USER_INFO_URI = "missing_user_info_uri";
    private static final String ERROR_CODE_MISSING_USER_NAME_ATTRIBUTE = "missing_user_name_attribute";

    @Setter
    @NonNull
    private WebClient webClient = WebClient.create();

    @SuppressWarnings("unchecked")
    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        Assert.notNull(userRequest, "OAuth 2 user request must not be null");
        return Mono.defer(() -> {
            ClientRegistration clientRegistration = userRequest.getClientRegistration();
            String userInfoEndpointUri = getUserInfoEndpointUri(clientRegistration);
            String userNameAttributeName = getUsernameAttributeName(clientRegistration);

            return requestUserInfo(userInfoEndpointUri, userRequest.getAccessToken().getTokenValue())
                    .flatMap(attrs -> Mono.justOrEmpty((List<Map<String, Object>>) attrs.get("response")))
                    .switchIfEmpty(Mono.error(() -> {
                        String message = "Response attribute is missing";
                        OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_INVALID_USER_INFO_RESPONSE, message, null);
                        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
                    }))
                    .filter(response -> !response.isEmpty())
                    .switchIfEmpty(Mono.error(() -> {
                        String message = "Response attribute does not have any value";
                        OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_INVALID_USER_INFO_RESPONSE, message, null);
                        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
                    }))
                    .filter(response -> response.size() == 1)
                    .switchIfEmpty(Mono.error(() -> {
                        String message = "Response attribute has more then one value";
                        OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_INVALID_USER_INFO_RESPONSE, message, null);
                        throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
                    }))
                    .map(response -> response.get(0))
                    .map(attrs -> {
                        if (attrs.get("email") == null) {
                            Map<String, Object> newAttrs = new HashMap<>(attrs);
                            newAttrs.put("email", userRequest.getAdditionalParameters().get("email"));
                            attrs = newAttrs;
                        }
                        GrantedAuthority authority = new OAuth2UserAuthority(attrs);
                        Set<GrantedAuthority> authorities = new HashSet<>();
                        authorities.add(authority);
                        OAuth2AccessToken token = userRequest.getAccessToken();
                        for (String scope : token.getScopes()) {
                            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
                        }
                        return new DefaultOAuth2User(authorities, attrs, userNameAttributeName);
                    })
                    .onErrorMap(e -> e instanceof IOException, t -> {
                        String message = "Unable to access the user info endpoint " + userInfoEndpointUri;
                        return new AuthenticationServiceException(message, t);
                    })
                    .onErrorMap(t -> !(t instanceof AuthenticationServiceException), t -> {
                        String message = "An error occurred while reading user info success response: "
                                + t.getMessage();
                        OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_INVALID_USER_INFO_RESPONSE, message, null);
                        return new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString(), t);
                    });
        });
    }

    private String getUserInfoEndpointUri(ClientRegistration clientRegistration) {
        ClientRegistration.ProviderDetails providerDetails = clientRegistration.getProviderDetails();
        ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint = providerDetails.getUserInfoEndpoint();
        String uri = userInfoEndpoint.getUri();
        if (!StringUtils.hasText(uri)) {
            String message = "Missing required user info URI in user info endpoint for "
                    + "client registration with id " + clientRegistration.getRegistrationId();
            OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_MISSING_USER_INFO_URI, message, null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        return uri;
    }

    private String getUsernameAttributeName(ClientRegistration clientRegistration) {
        ClientRegistration.ProviderDetails providerDetails = clientRegistration.getProviderDetails();
        ClientRegistration.ProviderDetails.UserInfoEndpoint userInfoEndpoint = providerDetails.getUserInfoEndpoint();
        String userNameAttributeName = userInfoEndpoint.getUserNameAttributeName();
        if (!StringUtils.hasText(userNameAttributeName)) {
            String message = "Missing required username attribute name in user info endpoint for "
                    + "client registration with id " + clientRegistration.getRegistrationId();
            OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_MISSING_USER_NAME_ATTRIBUTE, message, null);
            throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
        }
        return userNameAttributeName;
    }

    private Mono<Map<String, Object>> requestUserInfo(String uri, String accessToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("access_token", accessToken);
        formData.add("fields", "photo_100");
        formData.add("v", "5.103");

        return webClient.post()
                .uri(uri)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(s -> s != HttpStatus.OK, response -> parse(response).map(errorResponse -> {
                    String description = errorResponse.getErrorObject().getDescription();
                    OAuth2Error oauth2Error = new OAuth2Error(ERROR_CODE_INVALID_USER_INFO_RESPONSE, description, null);
                    throw new OAuth2AuthenticationException(oauth2Error, oauth2Error.toString());
                }))
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }

    private static Mono<UserInfoErrorResponse> parse(ClientResponse httpResponse) {
        String wwwAuth = httpResponse.headers().asHttpHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE);
        if (!StringUtils.isEmpty(wwwAuth)) {
            return Mono.fromCallable(() -> UserInfoErrorResponse.parse(wwwAuth));
        }

        ParameterizedTypeReference<Map<String, String>> typeReference = new ParameterizedTypeReference<>() {
        };
        return httpResponse
                .bodyToMono(typeReference)
                .map(body -> new UserInfoErrorResponse(ErrorObject.parse(new JSONObject(body))));
    }
}
