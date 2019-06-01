package org.briarheart.orchestra.config;

import org.briarheart.orchestra.model.Task;
import org.briarheart.orchestra.model.TaskComment;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;

@Configuration
public class RepositoryRestConfig implements RepositoryRestConfigurer {
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.exposeIdsFor(Task.class, TaskComment.class);
    }
}
