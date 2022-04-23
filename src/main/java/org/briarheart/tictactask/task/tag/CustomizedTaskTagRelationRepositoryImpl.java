package org.briarheart.tictactask.task.tag;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.tictactask.util.DateTimeUtils;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @author Roman Chigvintsev
 */
@Component
public class CustomizedTaskTagRelationRepositoryImpl implements CustomizedTaskTagRelationRepository {
    @SuppressWarnings("SqlResolve")
    private static final String SQL_CREATE_TASK_TAG_RELATION = "INSERT INTO tasks_tags (task_id, tag_id, created_at) "
            + "VALUES (:taskId, :tagId, :createdAt)";

    private final DatabaseClient databaseClient;

    public CustomizedTaskTagRelationRepositoryImpl(DatabaseClient databaseClient) {
        Assert.notNull(databaseClient, "Database client must not be null");
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<TaskTagRelation> create(Long taskId, Long tagId) {
        LocalDateTime createdAt = DateTimeUtils.currentDateTimeUtc();
        return databaseClient.sql(SQL_CREATE_TASK_TAG_RELATION)
                .bind("taskId", taskId)
                .bind("tagId", tagId)
                .bind("createdAt", createdAt)
                .fetch()
                .first()
                .map(result -> new TaskTagRelation(taskId, tagId, createdAt));
    }
}
