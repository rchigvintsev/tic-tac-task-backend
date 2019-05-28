package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Roman Chigvintsev
 */
@RepositoryRestResource
public interface TaskCommentRepository extends CrudRepository<TaskComment, Long> {
    Page<TaskComment> findByTaskId(@Param("taskId") Long taskId, Pageable pageable);
}
