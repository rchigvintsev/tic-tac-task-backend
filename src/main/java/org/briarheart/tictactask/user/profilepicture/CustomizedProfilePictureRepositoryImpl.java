package org.briarheart.tictactask.user.profilepicture;

import io.jsonwebtoken.lang.Assert;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
@Component
public class CustomizedProfilePictureRepositoryImpl implements CustomizedProfilePictureRepository {
    @SuppressWarnings("SqlResolve")
    private static final String SQL_CREATE_PROFILE_PICTURE = "INSERT INTO profile_picture (user_id, data, type) "
            + "VALUES (:userId, :data, :type)";

    private final DatabaseClient databaseClient;

    public CustomizedProfilePictureRepositoryImpl(DatabaseClient databaseClient) {
        Assert.notNull(databaseClient, "Database client must not be null");
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<ProfilePicture> create(ProfilePicture picture) {
        return databaseClient.sql(SQL_CREATE_PROFILE_PICTURE)
                .bind("userId", picture.getUserId())
                .bind("data", picture.getData())
                .bind("type", picture.getType())
                .fetch()
                .first()
                .map(result -> picture);
    }
}
