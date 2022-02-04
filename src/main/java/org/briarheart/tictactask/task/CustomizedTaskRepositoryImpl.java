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
        Criteria criteria = Criteria.where("status").in(request.getStatuses())
                .and("user_id").is(user.getId());

        if (request.isDeadlineFromDirty()) {
            if (request.getDeadlineFrom() != null) {
                criteria = criteria.and("deadline").greaterThanOrEquals(request.getDeadlineFrom());
            } else {
                criteria = criteria.and("deadline").isNull();
            }
        }

        if (request.isDeadlineToDirty()) {
            if (request.getDeadlineTo() != null) {
                criteria = criteria.and("deadline").lessThanOrEquals(request.getDeadlineTo());
            } else {
                criteria = criteria.and("deadline").isNull();
            }
        }

        if (request.getCompletedAtFrom() != null) {
            criteria = criteria.and("completed_at").greaterThanOrEquals(request.getCompletedAtFrom());
        }

        if (request.getCompletedAtTo() != null) {
            criteria = criteria.and("completed_at").lessThanOrEquals(request.getCompletedAtTo());
        }

        return criteria;
    }
}
