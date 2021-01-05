CREATE TABLE authorities (
    email VARCHAR(255) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT pk_authorities PRIMARY KEY (email, authority),
    CONSTRAINT fk_authorities_users FOREIGN KEY (email) REFERENCES users(email)
)
