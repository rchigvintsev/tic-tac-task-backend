-- noinspection SqlResolve
CREATE TABLE task_comment (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT REFERENCES task(id) ON DELETE CASCADE,
  comment_text VARCHAR(10000) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP
)
