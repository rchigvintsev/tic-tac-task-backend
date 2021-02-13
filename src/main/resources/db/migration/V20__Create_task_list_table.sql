-- noinspection SqlResolve
CREATE TABLE task_list (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  completed BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_task_list_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
