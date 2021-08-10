package org.briarheart.orchestra.security.oauth2.client.userinfo;

import io.jsonwebtoken.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.briarheart.orchestra.security.oauth2.core.user.OAuth2UserAttributeAccessor;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;

/**
 * Implementation of {@link ReactiveOAuth2UserService} that delegates loading of OAuth 2 user attributes to one of
 * {@link ReactiveOAuth2UserLoader}. This service saves successfully loaded user attributes in the database.
 *
 * @author Roman Chigvintsev
 * @see ReactiveOAuth2UserLoader
 * @see UserRepository
 * @see User
 */
@Slf4j
public class ReactiveOAuth2UserLoaderManager<R extends OAuth2UserRequest, U extends OAuth2User>
        implements ReactiveOAuth2UserService<R, U> {
    private static final String MISSING_EMAIL_ATTRIBUTE_ERROR_CODE = "missing_email";

    private final List<ReactiveOAuth2UserLoader<R, U>> userLoaders;
    private final UserRepository userRepository;

    /**
     * Creates new instance of this class with the given user loaders and user repository.
     *
     * @param userLoaders    user loaders (must not be {@code null} or empty)
     * @param userRepository user repository (must not be {@code null})
     */
    public ReactiveOAuth2UserLoaderManager(List<ReactiveOAuth2UserLoader<R, U>> userLoaders,
                                           UserRepository userRepository) {
        Assert.notEmpty(userLoaders, "User loaders must not be null or empty");
        Assert.notNull(userRepository, "User repository must not be null");

        this.userLoaders = userLoaders;
        this.userRepository = userRepository;
    }


    /**
     * Returns an instance of {@link OAuth2User} after obtaining attributes of the End-User from the UserInfo Endpoint.
     *
     * @param userRequest user request (must not be {@code null})
     * @return an instance of {@link OAuth2User}
     * @throws OAuth2AuthenticationException if {@link ReactiveOAuth2UserLoader} supporting the given client
     *                                       registration is not found or if an error occurs while attempting to obtain
     *                                       user attributes or if loaded user attributes do not include email
     */
    @Override
    public Mono<U> loadUser(R userRequest) throws OAuth2AuthenticationException {
        return userLoaders.stream()
                .filter(userLoader -> userLoader.supports(userRequest.getClientRegistration()))
                .findAny()
                .map(userLoader -> {
                    Mono<U> userMono = userLoader.loadUser(userRequest);
                    return userMono.zipWhen(user -> {
                        OAuth2UserAttributeAccessor attrAccessor = (OAuth2UserAttributeAccessor) user;
                        if (!StringUtils.hasText(attrAccessor.getEmail())) {
                            OAuth2Error oauth2Error = new OAuth2Error(MISSING_EMAIL_ATTRIBUTE_ERROR_CODE,
                                    "OAuth2 user \"" + user.getName() + "\" does not have an email", null);
                            throw new OAuth2AuthenticationException(oauth2Error);
                        }
                        return userRepository.findByEmail(attrAccessor.getEmail())
                                .switchIfEmpty(createNewUser(attrAccessor));
                    }).map(Tuple2::getT1);
                }).orElseThrow(() -> {
                    String clientRegId = userRequest.getClientRegistration().getRegistrationId();
                    OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "OAuth2 user loader that"
                            + " supports client registration with id \"" + clientRegId + "\" is not found", null);
                    throw new OAuth2AuthenticationException(oauth2Error);
                });
    }

    private Mono<? extends User> createNewUser(OAuth2UserAttributeAccessor attrAccessor) {
        return Mono.defer(() -> {
            User newUser = User.builder()
                    .email(attrAccessor.getEmail())
                    .emailConfirmed(true)
                    .enabled(true)
                    .fullName(attrAccessor.getFullName())
                    .profilePictureUrl(attrAccessor.getPicture())
                    .build();
            return userRepository.save(newUser).doOnSuccess(u -> log.debug("User with id {} is created", u.getId()));
        });
    }
}
