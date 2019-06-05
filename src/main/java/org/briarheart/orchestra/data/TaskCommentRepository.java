package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

/**
 * @author Roman Chigvintsev
 */
@RepositoryRestResource
public interface TaskCommentRepository extends CrudRepository<TaskComment, Long> {
    @RestResource(path = "findByTaskId")
    Page<TaskComment> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId, Pageable pageable);
}
