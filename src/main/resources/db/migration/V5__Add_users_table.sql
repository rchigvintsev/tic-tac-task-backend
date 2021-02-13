-- noinspection SqlResolve
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    version BIGINT NOT NULL,
    full_name VARCHAR(255),
    image_url VARCHAR(2000)
)
