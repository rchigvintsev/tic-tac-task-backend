package org.briarheart.tictactask.task;

import io.jsonwebtoken.lang.Assert;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.Pageables;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@Component
public class CustomizedTaskRepositoryImpl implements CustomizedTaskRepository {
    private final R2dbcEntityTemplate entityTemplate;

    public CustomizedTaskRepositoryImpl(R2dbcEntityTemplate entityTemplate) {
        Assert.notNull(entityTemplate, "Entity template must not be null");
        this.entityTemplate = entityTemplate;
    }

    @Override
    public Mono<Long> count(GetTasksRequest request, User user) {
        Assert.notNull(request, "Request must not be null");
        Assert.notNull(user, "User must not be null");

        DSLContext create = DSL.using(SQLDialect.POSTGRES);
        SelectConditionStep<Record1<Integer>> query = create.selectCount()
                .from(table("task"))
                .where(field("user_id").eq(user.getId()));
        Condition statusCondition = DSL.noCondition();
        for (TaskStatus status : request.getStatuses()) {
            statusCondition = statusCondition.or(getTaskStatusCondition(status, request));
        }
        query = query.and(statusCondition);

        DatabaseClient.GenericExecuteSpec executeSpec = createExecuteSpec(query);
        return executeSpec.map((row, rowMetadata) -> row.get(0, Long.class)).one();
    }

    @Override
    public Flux<Task> find(GetTasksRequest request, User user, Pageable pageable) {
        Assert.notNull(request, "Request must not be null");
        Assert.notNull(user, "User must not be null");

        List<Field<Object>> taskFields = getAllFields();
        DSLContext create = DSL.using(SQLDialect.POSTGRES);
        Query query = create.select(taskFields)
                .from(table("task"))
                .where(field("user_id").eq(user.getId()));
        Condition statusCondition = DSL.noCondition();
        for (TaskStatus status : request.getStatuses()) {
            statusCondition = statusCondition.or(getTaskStatusCondition(status, request));
        }
        query = ((SelectConditionStep<?>) query).and(statusCondition)
                .orderBy(field("created_at").desc())
                .offset(Pageables.getOffset(pageable));

        Integer limit = Pageables.getLimit(pageable);
        if (limit != null) {
            query = ((SelectLimitAfterOffsetStep<?>) query).limit(limit);
        }

        DatabaseClient.GenericExecuteSpec executeSpec = createExecuteSpec(query);
        BiFunction<Row, RowMetadata, Task> rowMapper = entityTemplate.getDataAccessStrategy().getRowMapper(Task.class);
        return executeSpec.map((rowMapper)).all();
    }

    @SuppressWarnings("deprecation")
    private List<Field<Object>> getAllFields() {
        ReactiveDataAccessStrategy dataAccessStrategy = entityTemplate.getDataAccessStrategy();
        List<SqlIdentifier> taskColumns = dataAccessStrategy.getAllColumns(Task.class);
        return taskColumns.stream()
                .map(column -> field(column.toSql(IdentifierProcessing.ANSI)))
                .toList();
    }

    private Condition getTaskStatusCondition(TaskStatus status, GetTasksRequest request) {
        Condition condition = field("status").eq(status.name());
        if (status == TaskStatus.PROCESSED || status == TaskStatus.COMPLETED) {
            if (status == TaskStatus.COMPLETED) {
                Set<String> previousStatuses = request.getStatuses().stream()
                        .filter(s -> s != TaskStatus.COMPLETED)
                        .map(TaskStatus::name)
                        .collect(Collectors.toSet());
                if (!previousStatuses.isEmpty()) {
                    condition = condition.and(field("previous_status").in(previousStatuses));
                }

                LocalDateTime completedAtFrom = request.getCompletedAtFrom();
                if (completedAtFrom != null) {
                    condition = condition.and(field("completed_at").ge(completedAtFrom));
                }

                LocalDateTime completedAtTo = request.getCompletedAtTo();
                if (completedAtTo != null) {
                    condition = condition.and(field("completed_at").le(completedAtTo));
                }
            }
            condition = condition.and(getTaskDeadlineCondition(request));
        }
        return condition;
    }

    private Condition getTaskDeadlineCondition(GetTasksRequest request) {
        if (request.isWithoutDeadline()) {
            return field("deadline_date").isNull().and(field("deadline_date_time").isNull());
        }

        Condition dateCondition = DSL.noCondition();
        LocalDate deadlineDateFrom = request.getDeadlineDateFrom();
        if (deadlineDateFrom != null) {
            dateCondition = dateCondition.and(field("deadline_date").ge(deadlineDateFrom));
        }
        LocalDate deadlineDateTo = request.getDeadlineDateTo();
        if (deadlineDateTo != null) {
            dateCondition = dateCondition.and(field("deadline_date").le(deadlineDateTo));
        }

        Condition dateTimeCondition = DSL.noCondition();
        LocalDateTime deadlineDateTimeFrom = request.getDeadlineDateTimeFrom();
        if (deadlineDateTimeFrom != null) {
            dateTimeCondition = dateTimeCondition.and(field("deadline_date_time").ge(deadlineDateTimeFrom));
        }
        LocalDateTime deadlineDateTimeTo = request.getDeadlineDateTimeTo();
        if (deadlineDateTimeTo != null) {
            dateTimeCondition = dateTimeCondition.and(field("deadline_date_time").le(deadlineDateTimeTo));
        }

        return dateCondition.or(dateTimeCondition);
    }

    private DatabaseClient.GenericExecuteSpec createExecuteSpec(Query query) {
        DatabaseClient databaseClient = entityTemplate.getDatabaseClient();
        return databaseClient.sql(query.toString());
    }
}
