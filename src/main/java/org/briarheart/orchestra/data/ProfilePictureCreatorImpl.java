package org.briarheart.orchestra.data;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.orchestra.model.ProfilePicture;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author Roman Chigvintsev
 */
@Component
public class ProfilePictureCreatorImpl implements ProfilePictureCreator {
    @SuppressWarnings("SqlResolve")
    private static final String SQL_CREATE_PROFILE_PICTURE = "INSERT INTO profile_picture (user_id, data, type) "
            + "VALUES (?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public ProfilePictureCreatorImpl(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JDBC template must not be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Mono<ProfilePicture> create(ProfilePicture picture) {
        ProfilePicture result = jdbcTemplate.execute(SQL_CREATE_PROFILE_PICTURE,
                (PreparedStatementCallback<ProfilePicture>) preparedStatement -> {
                    preparedStatement.setLong(1, picture.getUserId());
                    preparedStatement.setBytes(2, picture.getData());
                    preparedStatement.setString(3, picture.getType());
                    preparedStatement.executeUpdate();
                    return picture;
                });
        return Mono.just(Objects.requireNonNull(result));
    }
}
