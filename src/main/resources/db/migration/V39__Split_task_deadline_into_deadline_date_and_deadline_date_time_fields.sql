-- noinspection SqlResolveForFile

ALTER TABLE task ADD deadline_date DATE;
ALTER TABLE task ADD deadline_date_time TIMESTAMP;

UPDATE task SET deadline_date = deadline::DATE WHERE deadline IS NOT NULL AND deadline_time_specified = FALSE;
UPDATE task SET deadline_date_time = deadline WHERE deadline IS NOT NULL AND deadline_time_specified = TRUE;

ALTER TABLE task DROP COLUMN deadline;
ALTER TABLE task DROP COLUMN deadline_time_specified;
