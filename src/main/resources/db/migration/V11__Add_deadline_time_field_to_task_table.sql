-- noinspection SqlResolve
-- noinspection SqlWithoutWhereForFile
ALTER TABLE task ADD deadline_time TIME;
UPDATE task SET deadline_time = deadline;
