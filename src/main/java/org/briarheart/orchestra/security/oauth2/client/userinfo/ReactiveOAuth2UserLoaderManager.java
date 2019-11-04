package org.briarheart.orchestra.security.oauth2.client.userinfo;

import lombok.RequiredArgsConstructor;
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
import java.util.Objects;

/**
 * Implementation of {@link ReactiveOAuth2UserService} that delegates loading of OAuth 2 user attributes to one of
 * {@link ReactiveOAuth2UserLoader}. This service saves successfully loaded user attributes in the database.
 *
 * @author Roman Chigvintsev
 * @see ReactiveOAuth2UserLoader
 * @see UserRepository
 * @see User
 */
@RequiredArgsConstructor
public class ReactiveOAuth2UserLoaderManager<R extends OAuth2UserRequest, U extends OAuth2User>
        implements ReactiveOAuth2UserService<R, U> {
    private static final String MISSING_EMAIL_ATTRIBUTE_ERROR_CODE = "missing_email";

    private final List<ReactiveOAuth2UserLoader<R, U>> userLoaders;
    private final UserRepository userRepository;

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
                        // TODO: compare current authentication provider with the one saved in the database since
                        //  user may at first sign up with Facebook and then sign up again with Google while using
                        //  the same email.
                        return userRepository.findById(attrAccessor.getEmail())
                                .switchIfEmpty(Mono.defer(() -> createNewUser(attrAccessor)))
                                .flatMap(u -> updateUserIfNecessary(attrAccessor, u));
                    }).map(Tuple2::getT1);
                }).orElseThrow(() -> {
                    String clientRegId = userRequest.getClientRegistration().getRegistrationId();
                    OAuth2Error oauth2Error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, "OAuth2 user loader that"
                            + " supports client registration with id \"" + clientRegId + "\" is not found", null);
                    throw new OAuth2AuthenticationException(oauth2Error);
                });
    }

    private Mono<? extends User> createNewUser(OAuth2UserAttributeAccessor attrAccessor) {
        User newUser = new User(attrAccessor.getEmail(), 0L, attrAccessor.getFullName(), attrAccessor.getPicture());
        return userRepository.save(newUser);
    }

    private Mono<User> updateUserIfNecessary(OAuth2UserAttributeAccessor attrAccessor, User user) {
        boolean needUpdate = false;

        if (!Objects.equals(user.getFullName(), attrAccessor.getFullName())) {
            user.setFullName(attrAccessor.getFullName());
            needUpdate = true;
        }

        if (!Objects.equals(user.getImageUrl(), attrAccessor.getPicture())) {
            user.setImageUrl(attrAccessor.getPicture());
            needUpdate = true;
        }

        if (needUpdate) {
            user.setVersion(user.getVersion() + 1);
            return userRepository.save(user);
        }

        return Mono.just(user);
    }
}
