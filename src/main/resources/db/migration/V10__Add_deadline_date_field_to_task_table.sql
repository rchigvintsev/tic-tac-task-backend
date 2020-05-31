-- noinspection SqlResolve
-- noinspection SqlWithoutWhereForFile
ALTER TABLE task ADD deadline_date DATE;
UPDATE task SET deadline_date = deadline;
