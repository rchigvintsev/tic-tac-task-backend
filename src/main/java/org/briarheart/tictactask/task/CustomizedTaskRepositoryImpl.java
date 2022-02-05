package org.briarheart.tictactask.task;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

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
        return entityTemplate.count(Query.query(buildCriteria(request, user)), Task.class);
    }

    @Override
    public Flux<Task> find(GetTasksRequest request, User user, Pageable pageable) {
        Assert.notNull(request, "Request must not be null");
        Assert.notNull(user, "User must not be null");

        Criteria criteria = buildCriteria(request, user);
        long offset = Pageables.getOffset(pageable);
        Integer limit = Pageables.getLimit(pageable);

        Query query = Query.query(criteria).offset(offset);
        if (limit != null) {
            query = query.limit(limit);
        }
        return entityTemplate.select(Task.class).matching(query).all();
    }

    private Criteria buildCriteria(GetTasksRequest request, User user) {
        Criteria criteria = Criteria.where("user_id").is(user.getId());
        Criteria statusCriteria = Criteria.empty();
        for (TaskStatus status : request.getStatuses()) {
            statusCriteria = statusCriteria.or(buildCriteriaForTaskStatus(status, request));
        }
        return criteria.and(statusCriteria);
    }

    private Criteria buildCriteriaForTaskStatus(TaskStatus status, GetTasksRequest request) {
        switch (status) {
            case UNPROCESSED:
                return Criteria.where("status").is(TaskStatus.UNPROCESSED);
            case PROCESSED: {
                Criteria criteria = Criteria.where("status").is(TaskStatus.PROCESSED);

                if (request.isDeadlineFromDirty()) {
                    LocalDateTime deadlineFrom = request.getDeadlineFrom();
                    if (deadlineFrom != null) {
                        criteria = criteria.and("deadline").greaterThanOrEquals(deadlineFrom);
                    } else {
                        criteria = criteria.and("deadline").isNull();
                    }
                }

                if (request.isDeadlineToDirty()) {
                    LocalDateTime deadlineTo = request.getDeadlineTo();
                    if (deadlineTo != null) {
                        criteria = criteria.and("deadline").lessThanOrEquals(deadlineTo);
                    } else if (!request.isDeadlineFromDirty() || request.getDeadlineFrom() != null) {
                        criteria = criteria.and("deadline").isNull();
                    }
                }

                return criteria;
            }
            case COMPLETED: {
                Criteria criteria = Criteria.where("status").is(TaskStatus.COMPLETED);

                LocalDateTime completedAtFrom = request.getCompletedAtFrom();
                LocalDateTime completedAtTo = request.getCompletedAtTo();

                if (completedAtFrom != null || completedAtTo != null) {
                    if (completedAtFrom != null) {
                        criteria = criteria.and("completed_at").greaterThanOrEquals(completedAtFrom);
                    }
                    if (completedAtTo != null) {
                        criteria = criteria.and("completed_at").lessThanOrEquals(completedAtTo);
                    }
                }

                return criteria;
            }
            default:
                return Criteria.empty();
        }
    }
}
