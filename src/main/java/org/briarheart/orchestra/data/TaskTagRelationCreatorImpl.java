package org.briarheart.orchestra.data;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.orchestra.model.TaskTagRelation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * @author Roman Chigvintsev
 */
@Component
public class TaskTagRelationCreatorImpl implements TaskTagRelationCreator {
    @SuppressWarnings("SqlResolve")
    private static final String SQL_CREATE_TASK_TAG_RELATION = "INSERT INTO tasks_tags (task_id, tag_id) VALUES (?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public TaskTagRelationCreatorImpl(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JDBC template must not be null");
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Mono<TaskTagRelation> create(Long taskId, Long tagId) {
        TaskTagRelation result = jdbcTemplate.execute(SQL_CREATE_TASK_TAG_RELATION,
                (PreparedStatementCallback<TaskTagRelation>) preparedStatement -> {
                    preparedStatement.setLong(1, taskId);
                    preparedStatement.setLong(2, tagId);
                    preparedStatement.executeUpdate();
                    return new TaskTagRelation(taskId, tagId);
                });
        return Mono.just(Objects.requireNonNull(result));
    }
}
