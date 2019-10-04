package org.briarheart.orchestra.config;

import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.security.oauth2.client.oidc.userinfo.DatabaseOidcReactiveOAuth2UserService;
import org.briarheart.orchestra.security.oauth2.client.web.server.CookieOAuth2ServerAuthorizationRequestRepository;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenReactiveAuthenticationManager;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationConverter;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationSuccessHandler;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.server.OAuth2AuthorizationRequestRedirectWebFilter;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.WebFilter;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationSuccessHandler.DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;

/**
 * @author Roman Chigvintsev
 */
@EnableWebFluxSecurity
public class WebSecurityConfig {
    @Value("${application.security.authentication.access-token.signing-key}")
    private String accessTokenSigningKey;

    @Value("${application.security.authentication.access-token.validity-seconds}")
    private long accessTokenValiditySeconds;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            ServerAuthenticationConverter authenticationConverter,
                                                            ServerAuthenticationSuccessHandler authenticationSuccessHandler,
                                                            AuthenticationWebFilter accessTokenAuthenticationWebFilter) {
        SecurityWebFilterChain filterChain = http.cors().configurationSource(createCorsConfigurationSource())
                .and()
                    .authorizeExchange().anyExchange().authenticated()
                .and()
                    .exceptionHandling()
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                    .oauth2Login()
                        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                        .authenticationConverter(authenticationConverter)
                        .authenticationSuccessHandler(authenticationSuccessHandler)
                .and()
                    .addFilterBefore(accessTokenAuthenticationWebFilter, SecurityWebFiltersOrder.HTTP_BASIC)
                    .build();
        configureOAuth2AuthorizationRequestRedirectWebFilter(filterChain);
        return filterChain;
    }

    @Bean
    public ServerAuthenticationConverter authenticationConverter(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
    ) {
        ServerOAuth2AuthorizationCodeAuthenticationTokenConverter converter
                = new ServerOAuth2AuthorizationCodeAuthenticationTokenConverter(clientRegistrationRepository);
        converter.setAuthorizationRequestRepository(authorizationRequestRepository);
        return converter;
    }

    @Bean
    public ReactiveClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        Collection<ClientRegistration> clientRegistrations = OAuth2ClientPropertiesRegistrationAdapter
                .getClientRegistrations(properties).values();
        return new InMemoryReactiveClientRegistrationRepository(new ArrayList<>(clientRegistrations));
    }

    @Bean
    public ServerAuthenticationSuccessHandler authenticationSuccessHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            UserRepository userRepository,
            AccessTokenService accessTokenService
    ) {
        return new AccessTokenServerAuthenticationSuccessHandler(authorizationRequestRepository, userRepository,
                accessTokenService);
    }

    @Bean
    public ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new CookieOAuth2ServerAuthorizationRequestRepository(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME);
    }

    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcOAuth2UserService(UserRepository userRepository) {
        return new DatabaseOidcReactiveOAuth2UserService(userRepository);
    }

    @Bean
    public AuthenticationWebFilter accessTokenAuthenticationWebFilter(AccessTokenService accessTokenService) {
        AccessTokenReactiveAuthenticationManager authenticationManager
                = new AccessTokenReactiveAuthenticationManager(accessTokenService);
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);
        filter.setServerAuthenticationConverter(new AccessTokenServerAuthenticationConverter());
        return filter;
    }

    @Bean
    public AccessTokenService accessTokenService() {
        JwtService tokenService = new JwtService(accessTokenSigningKey.getBytes(StandardCharsets.UTF_8));
        tokenService.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        return tokenService;
    }

    private CorsConfigurationSource createCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // TODO: save client origin somewhere in the application settings
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedHeader("*");
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.HEAD.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void configureOAuth2AuthorizationRequestRedirectWebFilter(SecurityWebFilterChain filterChain) {
        getWebFilter(filterChain, OAuth2AuthorizationRequestRedirectWebFilter.class).ifPresent(filter -> {
            filter.setAuthorizationRequestRepository(authorizationRequestRepository());
            filter.setRequestCache(NoOpServerRequestCache.getInstance());
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends WebFilter> Optional<T> getWebFilter(SecurityWebFilterChain filterChain, Class<T> filterClass) {
        return (Optional<T>) filterChain.getWebFilters()
                .filter(Objects::nonNull)
                .filter(filter -> filter.getClass().isAssignableFrom(filterClass))
                .singleOrEmpty()
                .blockOptional();
    }
}
