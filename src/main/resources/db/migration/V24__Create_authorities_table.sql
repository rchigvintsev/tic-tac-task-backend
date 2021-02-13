-- noinspection SqlResolve
CREATE TABLE authorities (
    user_id BIGINT NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT pk_authorities PRIMARY KEY (user_id, authority),
    CONSTRAINT fk_authorities_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
