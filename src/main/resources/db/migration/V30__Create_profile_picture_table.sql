-- noinspection SqlResolve
CREATE TABLE profile_picture (
  user_id BIGINT PRIMARY KEY,
  data BYTEA NOT NULL,
  type VARCHAR(20),
  CONSTRAINT fk_profile_picture_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
