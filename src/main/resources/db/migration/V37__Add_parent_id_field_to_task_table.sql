-- noinspection SqlResolveForFile

ALTER TABLE task ADD parent_id BIGINT REFERENCES task(id);
