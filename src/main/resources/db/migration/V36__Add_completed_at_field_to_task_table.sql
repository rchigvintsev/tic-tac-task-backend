-- noinspection SqlResolveForFile

ALTER TABLE task ADD completed_at TIMESTAMP;
UPDATE task SET completed_at = (NOW() AT TIME ZONE 'utc') - INTERVAL '1 DAY';
