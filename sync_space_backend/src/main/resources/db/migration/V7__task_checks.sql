CREATE TABLE task_checks (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    done BOOLEAN NOT NULL,
    position INT NOT NULL
);

CREATE INDEX idx_task_checks_task_position ON task_checks (task_id, position);
