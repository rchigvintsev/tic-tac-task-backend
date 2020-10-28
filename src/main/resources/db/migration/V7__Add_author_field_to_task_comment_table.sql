-- noinspection SqlResolve
ALTER TABLE task_comment ADD author VARCHAR(255) NOT NULL DEFAULT 'unknown';
