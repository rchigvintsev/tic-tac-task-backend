package org.briarheart.orchestra.data;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.orchestra.model.TaskTagRelation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author Roman Chigvintsev
 */
@Component
public class TaskTagRelationCreatorImpl implements TaskTagRelationCreator {
    @SuppressWarnings("SqlResolve")
    private static final String SQL_CREATE_TASK_TAG_RELATION = "INSERT INTO tasks_tags (task_id, tag_id) "
            + "VALUES (:taskId, :tagId)";

    private final DatabaseClient databaseClient;

    public TaskTagRelationCreatorImpl(DatabaseClient databaseClient) {
        Assert.notNull(databaseClient, "Database client must not be null");
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<TaskTagRelation> create(Long taskId, Long tagId) {
        return databaseClient.sql(SQL_CREATE_TASK_TAG_RELATION)
                .bind("taskId", taskId)
                .bind("tagId", tagId)
                .fetch()
                .first()
                .map(result -> new TaskTagRelation(taskId, tagId));
    }
}
