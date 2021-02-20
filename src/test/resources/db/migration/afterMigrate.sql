-- noinspection SqlWithoutWhereForFile
-- noinspection SqlResolveForFile

DELETE FROM users;
INSERT INTO users (id, email, version, full_name) VALUES (1, 'john.doe@mail.com', 1, 'John Doe');

DELETE FROM tasks_tags;

DELETE FROM task;
INSERT INTO task (id, user_id, title, status) VALUES (1, 1, 'Test task', 'UNPROCESSED');

DELETE FROM tag;
INSERT INTO tag (id, user_id, name) VALUES (1, 1, 'Test tag');
