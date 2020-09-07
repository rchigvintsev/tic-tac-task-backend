CREATE TABLE tasks_tags (
  task_id BIGINT,
  tag_id BIGINT,
  CONSTRAINT pk_tasks_task_tags PRIMARY KEY (task_id, tag_id),
  CONSTRAINT fk_tasks_task_tags_task FOREIGN KEY (task_id) REFERENCES task(id),
  CONSTRAINT fk_tasks_task_tags_task_tag FOREIGN KEY (tag_id) REFERENCES tag(id)
)
