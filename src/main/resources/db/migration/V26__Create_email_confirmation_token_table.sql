-- noinspection SqlResolve
CREATE TABLE email_confirmation_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    token_value VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_email_confirmation_token_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
)
