-- noinspection SqlResolve
ALTER TABLE tag ADD CONSTRAINT uq_tag_name_author UNIQUE(name, author);
