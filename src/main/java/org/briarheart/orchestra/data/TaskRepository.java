package org.briarheart.orchestra.data;

import org.briarheart.orchestra.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByCompleted(@Param("completed") Boolean completed, Pageable pageable);
}
