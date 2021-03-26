-- noinspection SqlResolve
ALTER TABLE password_reset_confirmation_token ADD valid BOOLEAN NOT NULL DEFAULT TRUE;
