-- noinspection SqlResolve
CREATE TABLE image (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  data BYTEA NOT NULL,
  type VARCHAR(20),
  CONSTRAINT fk_image_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
