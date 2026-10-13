ALTER TABLE tasks
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_tasks_space_archived ON tasks (space_id, archived);
