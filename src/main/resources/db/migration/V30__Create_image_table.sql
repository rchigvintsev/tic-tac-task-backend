-- noinspection SqlResolve
CREATE TABLE image (
  id BIGSERIAL PRIMARY KEY,
  image_data BYTEA NOT NULL,
  user_id BIGINT NOT NULL,
  CONSTRAINT fk_image_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
