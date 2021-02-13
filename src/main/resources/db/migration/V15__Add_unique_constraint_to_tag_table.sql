-- noinspection SqlResolve
ALTER TABLE tag ADD CONSTRAINT uq_tag_name_user_id UNIQUE(name, user_id);
