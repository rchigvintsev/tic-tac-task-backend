package org.briarheart.orchestra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.briarheart.orchestra.data.UserAuthorityRelationRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.security.oauth2.client.endpoint.ReactiveAccessTokenTypeWebClientFilter;
import org.briarheart.orchestra.security.oauth2.client.userinfo.*;
import org.briarheart.orchestra.security.oauth2.client.web.server.CookieOAuth2ServerAuthorizationRequestRepository;
import org.briarheart.orchestra.security.oauth2.core.userdetails.DatabaseReactiveUserDetailsService;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenReactiveAuthenticationManager;
import org.briarheart.orchestra.security.web.server.authentication.AccessTokenServerAuthenticationConverter;
import org.briarheart.orchestra.security.web.server.authentication.ClientRedirectUriServerAuthenticationFailureHandler;
import org.briarheart.orchestra.security.web.server.authentication.ClientRedirectUriServerAuthenticationSuccessHandler;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.briarheart.orchestra.security.web.server.authentication.jwt.CookieJwtRepository;
import org.briarheart.orchestra.security.web.server.authentication.jwt.JwtService;
import org.briarheart.orchestra.security.web.server.authentication.logout.AccessTokenLogoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.server.ServerAuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.authentication.*;
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.briarheart.orchestra.security.web.server.authentication.ClientRedirectUriServerAuthenticationSuccessHandler.DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;
import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

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
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationConverter authenticationConverter,
            ServerAuthenticationSuccessHandler authenticationSuccessHandler,
            ServerAuthenticationFailureHandler authenticationFailureHandler,
            ServerLogoutHandler logoutHandler,
            AuthenticationWebFilter accessTokenAuthenticationWebFilter,
            AuthenticationWebFilter httpBasicAuthenticationWebFilter,
            ServerSecurityContextRepository securityContextRepository
    ) {
        return http.cors().configurationSource(createCorsConfigurationSource())
                .and()
                    .securityContextRepository(securityContextRepository)
                    .requestCache().disable()
                    .csrf().disable()
                    .authorizeExchange().anyExchange().authenticated()
                .and()
                    .exceptionHandling()
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                    .oauth2Login()
                        .authenticationConverter(authenticationConverter)
                        .authenticationSuccessHandler(authenticationSuccessHandler)
                        .authenticationFailureHandler(authenticationFailureHandler)
                        .authorizationRequestRepository(authorizationRequestRepository())
                .and()
                    .logout()
                        .logoutHandler(logoutHandler)
                        .logoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.OK))
                .and()
                    .addFilterBefore(accessTokenAuthenticationWebFilter, SecurityWebFiltersOrder.HTTP_BASIC)
                    .addFilterAt(httpBasicAuthenticationWebFilter, SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
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
            AccessTokenService accessTokenService,
            ObjectMapper objectMapper,
            ServerRedirectStrategy redirectStrategy
    ) {
        ClientRedirectUriServerAuthenticationSuccessHandler handler;
        handler = new ClientRedirectUriServerAuthenticationSuccessHandler(authorizationRequestRepository,
                userRepository, accessTokenService, objectMapper);
        handler.setRedirectStrategy(redirectStrategy);
        return handler;
    }

    @Bean
    public ServerAuthenticationFailureHandler authenticationFailureHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            ServerRedirectStrategy redirectStrategy
    ) {
        ClientRedirectUriServerAuthenticationFailureHandler handler;
        handler = new ClientRedirectUriServerAuthenticationFailureHandler(authorizationRequestRepository);
        handler.setRedirectStrategy(redirectStrategy);
        return handler;
    }

    @Bean
    public ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new CookieOAuth2ServerAuthorizationRequestRepository(DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME);
    }

    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcOAuth2UserService(UserRepository userRepository) {
        List<ReactiveOAuth2UserLoader<OidcUserRequest, OidcUser>> userLoaders;
        userLoaders = List.of(new GoogleReactiveOAuth2UserLoader(new OidcReactiveOAuth2UserService()));
        return new ReactiveOAuth2UserLoaderManager<>(userLoaders, userRepository);
    }

    @Bean
    public ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService(UserRepository userRepository) {
        DefaultReactiveOAuth2UserService userService = new DefaultReactiveOAuth2UserService();
        List<ReactiveOAuth2UserLoader<OAuth2UserRequest, OAuth2User>> userLoaders = List.of(
                new FacebookReactiveOAuth2UserLoader(userService),
                new GithubReactiveOAuth2UserLoader(userService),
                new VkReactiveOAuth2UserLoader(new VkReactiveOAuth2UserService())
        );
        return new ReactiveOAuth2UserLoaderManager<>(userLoaders, userRepository);
    }

    @Bean
    public AuthenticationWebFilter accessTokenAuthenticationWebFilter(
            AccessTokenService accessTokenService,
            ServerAccessTokenRepository accessTokenRepository,
            ServerSecurityContextRepository securityContextRepository
    ) {
        AccessTokenReactiveAuthenticationManager authenticationManager
                = new AccessTokenReactiveAuthenticationManager(accessTokenService);
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);
        filter.setServerAuthenticationConverter(new AccessTokenServerAuthenticationConverter(accessTokenRepository));
        filter.setSecurityContextRepository(securityContextRepository);
        return filter;
    }

    @Bean
    public AuthenticationWebFilter httpBasicAuthenticationWebFilter(
            ReactiveUserDetailsService userDetailsService,
            ServerAuthenticationFailureHandler authenticationFailureHandler,
            ServerSecurityContextRepository securityContextRepository
    ) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager
                = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        DelegatingPasswordEncoder passwordEncoder = (DelegatingPasswordEncoder) createDelegatingPasswordEncoder();
        passwordEncoder.setDefaultPasswordEncoderForMatches(new NeverMatchesPasswordEncoder());
        authenticationManager.setPasswordEncoder(passwordEncoder);
        ServerWebExchangeMatcher matcher = new PathPatternParserServerWebExchangeMatcher("/login", HttpMethod.POST);

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);
        filter.setAuthenticationFailureHandler(authenticationFailureHandler);
        filter.setSecurityContextRepository(securityContextRepository);
        filter.setRequiresAuthenticationMatcher(matcher);
        return filter;
    }

    @Bean
    public AccessTokenLogoutHandler accessTokenLogoutHandler(ServerAccessTokenRepository accessTokenRepository) {
        return new AccessTokenLogoutHandler(accessTokenRepository);
    }

    @Bean
    public ServerSecurityContextRepository securityContextRepository() {
        return NoOpServerSecurityContextRepository.getInstance();
    }

    @Bean
    public AccessTokenService accessTokenService(ServerAccessTokenRepository accessTokenRepository) {
        JwtService tokenService = new JwtService(accessTokenRepository, accessTokenSigningKey);
        tokenService.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
        return tokenService;
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService(
            UserRepository userRepository,
            UserAuthorityRelationRepository userAuthorityRelationRepository
    ) {
        return new DatabaseReactiveUserDetailsService(userRepository, userAuthorityRelationRepository);
    }

    @Bean
    public ServerAccessTokenRepository accessTokenRepository() {
        return new CookieJwtRepository();
    }

    @Bean
    public ReactiveOAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> oAuth2AccessTokenResponseClient() {
        WebClientReactiveAuthorizationCodeTokenResponseClient client;
        client = new WebClientReactiveAuthorizationCodeTokenResponseClient();
        client.setWebClient(WebClient.builder()
                .filter(new ReactiveAccessTokenTypeWebClientFilter())
                .build());
        return client;
    }

    @Bean
    public ServerRedirectStrategy redirectStrategy() {
        return new DefaultServerRedirectStrategy();
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

    /**
     * Default {@link PasswordEncoder} whose method {@link #matches(CharSequence, String)} always returns {@code false}.
     */
    private static class NeverMatchesPasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence rawPassword) {
            throw new UnsupportedOperationException("Password encoding is not supported");
        }

        @Override
        public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {
            return false;
        }
    }
}
