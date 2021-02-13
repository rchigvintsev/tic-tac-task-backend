-- noinspection SqlResolve
CREATE TABLE task_comment (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL,
  comment_text VARCHAR(10000) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  CONSTRAINT fk_task_comment_task FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
)
