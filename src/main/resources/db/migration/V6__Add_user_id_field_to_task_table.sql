-- noinspection SqlResolveForFile
ALTER TABLE task ADD user_id BIGINT NOT NULL DEFAULT -1;
ALTER TABLE task ADD CONSTRAINT fk_task_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
