-- noinspection SqlResolve
CREATE TABLE tag (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_tag_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
