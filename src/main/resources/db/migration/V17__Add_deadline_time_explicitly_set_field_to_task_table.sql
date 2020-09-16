-- noinspection SqlResolveForFile
ALTER TABLE task ADD deadline_time_explicitly_set BOOLEAN;
-- noinspection SqlWithoutWhere
UPDATE task SET deadline_time_explicitly_set = deadline_time IS NOT NULL;
