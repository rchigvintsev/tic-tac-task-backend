-- noinspection SqlResolve
ALTER TABLE task_comment ADD author VARCHAR(254) NOT NULL DEFAULT 'unknown';
