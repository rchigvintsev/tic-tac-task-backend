-- noinspection SqlWithoutWhereForFile
-- noinspection SqlResolveForFile

DELETE FROM users;
INSERT INTO users (email, version, full_name) VALUES ('john.doe@mail.com', 1, 'John Doe');

DELETE FROM tasks_tags;

DELETE FROM task;
INSERT INTO task (id, title, status, author) VALUES (1, 'Test task', 'UNPROCESSED', 'john.doe@mail.com');

DELETE FROM tag;
INSERT INTO tag (id, name, author) VALUES (1, 'Test tag', 'john.doe@mail.com');
