-- noinspection SqlResolveForFile

ALTER TABLE task ADD deadline_time_specified BOOLEAN;
-- noinspection SqlWithoutWhere
UPDATE task SET deadline_time_specified = deadline_time_explicitly_set;
ALTER TABLE task DROP COLUMN deadline_time_explicitly_set;
