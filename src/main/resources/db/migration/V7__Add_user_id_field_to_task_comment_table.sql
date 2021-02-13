-- noinspection SqlResolveForFile
ALTER TABLE task_comment ADD user_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE task_comment ADD CONSTRAINT fk_task_comment_users FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE;
