-- noinspection SqlResolve
CREATE TABLE users (
  email VARCHAR(255) PRIMARY KEY,
  version BIGINT NOT NULL,
  full_name VARCHAR(255),
  image_url VARCHAR(2000)
)
