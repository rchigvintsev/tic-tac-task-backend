package org.briarheart.tictactask.task;

import io.jsonwebtoken.lang.Assert;
import org.briarheart.tictactask.user.User;
import org.briarheart.tictactask.util.Pageables;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

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

        Query query = Query.query(criteria).offset(offset).sort(Sort.by("created_at").ascending());
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
        if (status == TaskStatus.UNPROCESSED) {
            return Criteria.where("status").is(TaskStatus.UNPROCESSED);
        }

        if (status == TaskStatus.PROCESSED || status == TaskStatus.COMPLETED) {
            Criteria criteria = Criteria.where("status").is(status).and(buildDeadlineCriteria(request));

            if (status == TaskStatus.COMPLETED) {
                Set<TaskStatus> previousStatuses = request.getStatuses().stream()
                        .filter(s -> s != TaskStatus.COMPLETED)
                        .collect(Collectors.toSet());
                if (!previousStatuses.isEmpty()) {
                    criteria = criteria.and("previous_status").in(previousStatuses);
                }

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
            }

            return criteria;
        }

        return Criteria.empty();
    }

    private Criteria buildDeadlineCriteria(GetTasksRequest request) {
        if (request.isWithoutDeadline()) {
            return Criteria.where("deadline_date").isNull().and("deadline_date_time").isNull();
        }

        Criteria deadlineCriteria = Criteria.empty();
        Criteria deadlineDateCriteria = Criteria.empty();

        if (request.getDeadlineDateFrom() != null) {
            deadlineDateCriteria = deadlineDateCriteria.and("deadline_date")
                    .greaterThanOrEquals(request.getDeadlineDateFrom());
        }

        if (request.getDeadlineDateTo() != null) {
            deadlineDateCriteria = deadlineDateCriteria.and("deadline_date")
                    .lessThanOrEquals(request.getDeadlineDateTo());
        }

        deadlineCriteria = deadlineCriteria.or(deadlineDateCriteria);
        Criteria deadlineDateTimeCriteria = Criteria.empty();

        if (request.getDeadlineDateTimeFrom() != null) {
            deadlineDateTimeCriteria = deadlineDateTimeCriteria.and("deadline_date_time")
                    .greaterThanOrEquals(request.getDeadlineDateTimeFrom());
        }

        if (request.getDeadlineDateTimeTo() != null) {
            deadlineDateTimeCriteria = deadlineDateTimeCriteria.and("deadline_date_time")
                    .lessThanOrEquals(request.getDeadlineDateTimeTo());
        }

        return deadlineCriteria.or(deadlineDateTimeCriteria);
    }
}
