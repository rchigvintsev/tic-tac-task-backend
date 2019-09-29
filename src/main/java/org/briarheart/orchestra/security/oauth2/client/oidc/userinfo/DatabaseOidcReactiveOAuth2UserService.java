package org.briarheart.orchestra.security.oauth2.client.oidc.userinfo;

import lombok.RequiredArgsConstructor;
import org.briarheart.orchestra.data.UserRepository;
import org.briarheart.orchestra.model.User;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Objects;

/**
 * OAuth2 user service that saves user info in the database.
 *
 * @author Roman Chigvintsev
 *
 * @see User
 * @see UserRepository
 */
@RequiredArgsConstructor
public class DatabaseOidcReactiveOAuth2UserService extends OidcReactiveOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public Mono<OidcUser> loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Mono<OidcUser> oidcUserMono = super.loadUser(userRequest);
        return oidcUserMono.zipWhen(oidcUser -> {
            OidcUserInfo userInfo = oidcUser.getUserInfo();
            if (userInfo != null) {
                return userRepository.findById(oidcUser.getEmail())
                        .switchIfEmpty(Mono.defer(() -> createNewUser(userInfo)))
                        .flatMap(u -> updateUserIfNecessary(oidcUser, u));
            }
            return Mono.just(User.EMPTY);
        }).map(Tuple2::getT1);
    }

    private Mono<? extends User> createNewUser(OidcUserInfo userInfo) {
        String picture = userInfo.getClaimAsString("picture");
        User newUser = new User(userInfo.getEmail(), 0L, userInfo.getFullName(), picture);
        return userRepository.save(newUser);
    }

    private Mono<User> updateUserIfNecessary(OidcUser oidcUser, User user) {
        boolean needUpdate = false;

        if (!Objects.equals(user.getFullName(), oidcUser.getFullName())) {
            user.setFullName(oidcUser.getFullName());
            needUpdate = true;
        }

        if (!Objects.equals(user.getImageUrl(), oidcUser.getClaimAsString("picture"))) {
            user.setImageUrl(oidcUser.getClaimAsString("picture"));
            needUpdate = true;
        }

        if (needUpdate) {
            user.setVersion(user.getVersion() + 1);
            return userRepository.save(user);
        }

        return Mono.just(user);
    }
}
