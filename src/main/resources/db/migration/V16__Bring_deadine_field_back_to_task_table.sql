-- noinspection SqlResolveForFile
-- noinspection SqlWithoutWhereForFile
ALTER TABLE task ADD deadline TIMESTAMP;
UPDATE task SET deadline = CASE
    WHEN deadline_date IS NOT NULL AND deadline_time IS NOT NULL THEN (deadline_date || ' ' || deadline_time)::TIMESTAMP
    WHEN deadline_date IS NOT NULL THEN deadline_date::TIMESTAMP
END;
