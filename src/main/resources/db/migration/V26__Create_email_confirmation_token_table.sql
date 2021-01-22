CREATE TABLE email_confirmation_token (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    token_value VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
)
