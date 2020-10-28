-- noinspection SqlResolveForFile
ALTER TABLE task ADD task_list_id BIGINT;
ALTER TABLE task ADD CONSTRAINT fk_task_task_list FOREIGN KEY (task_list_id) REFERENCES task_list (id);
