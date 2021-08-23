package org.briarheart.orchestra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.EmailConfirmationTokenRepository;
import org.briarheart.orchestra.data.PasswordResetConfirmationTokenRepository;
import org.briarheart.orchestra.data.UserAuthorityRelationRepository;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.security.oauth2.client.endpoint.ReactiveAccessTokenTypeWebClientFilter;
import org.briarheart.orchestra.security.oauth2.client.userinfo.*;
import org.briarheart.orchestra.security.oauth2.client.web.server.CookieOAuth2ServerAuthorizationRequestRepository;
import org.briarheart.orchestra.security.oauth2.client.web.server.CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter;
import org.briarheart.orchestra.security.oauth2.client.web.server.FacebookOAuth2AuthorizationResponseConverter;
import org.briarheart.orchestra.security.oauth2.client.web.server.OAuth2AuthorizationResponseConverter;
import org.briarheart.orchestra.security.oauth2.core.userdetails.DatabaseReactiveUserDetailsService;
import org.briarheart.orchestra.security.web.server.authentication.*;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.AccessTokenService;
import org.briarheart.orchestra.security.web.server.authentication.accesstoken.ServerAccessTokenRepository;
import org.briarheart.orchestra.security.web.server.authentication.jwt.CookieJwtRepository;
import org.briarheart.orchestra.security.web.server.authentication.jwt.JwtService;
import org.briarheart.orchestra.security.web.server.authentication.logout.AccessTokenLogoutHandler;
import org.briarheart.orchestra.service.DefaultEmailConfirmationService;
import org.briarheart.orchestra.service.DefaultPasswordService;
import org.briarheart.orchestra.service.EmailConfirmationService;
import org.briarheart.orchestra.service.PasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mail.javamail.JavaMailSender;
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
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.briarheart.orchestra.security.authorization.UnauthenticatedReactiveAuthorizationManager.unauthenticated;
import static org.briarheart.orchestra.security.web.server.authentication.ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler.DEFAULT_CLIENT_REDIRECT_URI_PARAMETER_NAME;
import static org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder;

/**
 * @author Roman Chigvintsev
 */
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final ApplicationSecurityProperties securityProperties;

    @Value("${spring.security.oauth2.client.redirect-uri-template}")
    private String clientRedirectUriTemplate;
    @Value("${application.security.cors.allowed-origin}")
    private String corsAllowedOrigin;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationConverter authenticationConverter,
            ServerAuthenticationSuccessHandler auth2LoginAuthenticationSuccessHandler,
            ServerAuthenticationFailureHandler auth2LoginAuthenticationFailureHandler,
            ServerLogoutHandler logoutHandler,
            AuthenticationWebFilter accessTokenAuthenticationWebFilter,
            AuthenticationWebFilter formLoginAuthenticationWebFilter,
            ServerSecurityContextRepository securityContextRepository
    ) {
        return http.redirectToHttps().httpsRedirectWhen(exchange -> {
                    ServerHttpRequest request = exchange.getRequest();
                    HttpHeaders headers = request.getHeaders();
                    return headers.getFirst("X-Forwarded-Proto") != null;
                })
                .and()
                    .cors().configurationSource(createCorsConfigurationSource(corsAllowedOrigin))
                .and()
                    .securityContextRepository(securityContextRepository)
                    .requestCache().disable()
                    .csrf().disable()
                    .authorizeExchange()
                        .pathMatchers(HttpMethod.POST, "/v?/users").access(unauthenticated())
                        .pathMatchers(HttpMethod.POST, "/v?/users/*/email/confirmation/*").access(unauthenticated())
                        .pathMatchers(HttpMethod.POST, "/v?/users/password/reset").access(unauthenticated())
                        .pathMatchers(HttpMethod.POST, "/v?/users/*/password/reset/confirmation/*").access(unauthenticated())
                        .anyExchange().authenticated()
                .and()
                    .exceptionHandling()
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .and()
                    .oauth2Login()
                        .authenticationConverter(authenticationConverter)
                        .authenticationSuccessHandler(auth2LoginAuthenticationSuccessHandler)
                        .authenticationFailureHandler(auth2LoginAuthenticationFailureHandler)
                        .authorizationRequestRepository(authorizationRequestRepository())
                .and()
                    .logout()
                        .logoutHandler(logoutHandler)
                        .logoutSuccessHandler(new HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.OK))
                .and()
                    .addFilterAt(formLoginAuthenticationWebFilter, SecurityWebFiltersOrder.FORM_LOGIN)
                    .addFilterAt(accessTokenAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public ServerAuthenticationConverter authenticationConverter(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository
    ) {
        Map<String, OAuth2AuthorizationResponseConverter> authResponseConverters = Map.of(
                "facebook", new FacebookOAuth2AuthorizationResponseConverter()
        );
        CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter converter;
        converter = new CustomServerOAuth2AuthorizationCodeAuthenticationTokenConverter(
                clientRegistrationRepository,
                authResponseConverters
        );
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
    public ServerAuthenticationSuccessHandler auth2LoginAuthenticationSuccessHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            UserRepository userRepository,
            AccessTokenService accessTokenService,
            ObjectMapper objectMapper,
            ServerRedirectStrategy redirectStrategy
    ) {
        ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler handler;
        handler = new ClientRedirectOAuth2LoginServerAuthenticationSuccessHandler(authorizationRequestRepository,
                clientRedirectUriTemplate, userRepository, accessTokenService, objectMapper);
        handler.setRedirectStrategy(redirectStrategy);
        return handler;
    }

    @Bean
    public ServerAuthenticationFailureHandler auth2LoginAuthenticationFailureHandler(
            ServerAuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository,
            ServerRedirectStrategy redirectStrategy
    ) {
        ClientRedirectOAuth2LoginServerAuthenticationFailureHandler handler;
        handler = new ClientRedirectOAuth2LoginServerAuthenticationFailureHandler(authorizationRequestRepository,
                clientRedirectUriTemplate);
        handler.setRedirectStrategy(redirectStrategy);
        return handler;
    }

    @Bean
    public ServerAuthenticationSuccessHandler formLoginAuthenticationSuccessHandler(
            AccessTokenService accessTokenService,
            ObjectMapper objectMapper
    ) {
        return new HttpStatusFormLoginServerAuthenticationSuccessHandler(accessTokenService, objectMapper);
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
            ServerAccessTokenRepository accessTokenRepository
    ) {
        AccessTokenReactiveAuthenticationManager authenticationManager
                = new AccessTokenReactiveAuthenticationManager(accessTokenService);
        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);
        filter.setServerAuthenticationConverter(new AccessTokenServerAuthenticationConverter(accessTokenRepository));
        return filter;
    }

    @Bean
    public AuthenticationWebFilter formLoginAuthenticationWebFilter(
            ReactiveUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            ServerAuthenticationSuccessHandler formLoginAuthenticationSuccessHandler
    ) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager
                = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authenticationManager.setPasswordEncoder(passwordEncoder);

        ServerAuthenticationConverter authenticationConverter = new ServerFormLoginAuthenticationConverter();

        ServerWebExchangeMatcher requiresAuthenticationMatcher
                = new PathPatternParserServerWebExchangeMatcher("/login", HttpMethod.POST);

        ServerAuthenticationEntryPoint entryPoint = new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED);
        ServerAuthenticationFailureHandler failureHandler
                = new ServerAuthenticationEntryPointFailureHandler(entryPoint);

        AuthenticationWebFilter filter = new AuthenticationWebFilter(authenticationManager);
        filter.setServerAuthenticationConverter(authenticationConverter);
        filter.setRequiresAuthenticationMatcher(requiresAuthenticationMatcher);
        filter.setAuthenticationSuccessHandler(formLoginAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(failureHandler);
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
        String signingKey = securityProperties.getAuthentication().getAccessToken().getSigningKey();
        long accessTokenValidity = securityProperties.getAuthentication().getAccessToken().getValiditySeconds();
        JwtService tokenService = new JwtService(accessTokenRepository, signingKey);
        tokenService.setAccessTokenValiditySeconds(accessTokenValidity);
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        DelegatingPasswordEncoder passwordEncoder = (DelegatingPasswordEncoder) createDelegatingPasswordEncoder();
        passwordEncoder.setDefaultPasswordEncoderForMatches(new NeverMatchesPasswordEncoder());
        return passwordEncoder;
    }

    @Bean
    public PasswordService passwordService(PasswordResetConfirmationTokenRepository tokenRepository,
                                           UserRepository userRepository,
                                           ApplicationInfoProperties applicationInfo,
                                           MessageSourceAccessor messages,
                                           JavaMailSender mailSender,
                                           PasswordEncoder passwordEncoder) {
        DefaultPasswordService passwordService = new DefaultPasswordService(tokenRepository, userRepository,
                applicationInfo, messages, mailSender, passwordEncoder);
        Duration tokenExpirationTimeout = securityProperties.getPasswordReset().getTokenExpirationTimeout();
        passwordService.setPasswordResetTokenExpirationTimeout(tokenExpirationTimeout);
        return passwordService;
    }

    @Bean
    public EmailConfirmationService emailConfirmationService(EmailConfirmationTokenRepository tokenRepository,
                                                             UserRepository userRepository,
                                                             ApplicationInfoProperties applicationInfo,
                                                             MessageSourceAccessor messages,
                                                             JavaMailSender mailSender) {
        DefaultEmailConfirmationService emailConfirmationService = new DefaultEmailConfirmationService(tokenRepository,
                userRepository, applicationInfo, messages, mailSender);
        Duration tokenExpirationTimeout = securityProperties.getEmailConfirmation().getTokenExpirationTimeout();
        emailConfirmationService.setEmailConfirmationTokenExpirationTimeout(tokenExpirationTimeout);
        return emailConfirmationService;
    }

    private CorsConfigurationSource createCorsConfigurationSource(String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin(allowedOrigin);
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
