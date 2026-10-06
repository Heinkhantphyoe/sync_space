CREATE TABLE task_activity (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users (id),
    kind VARCHAR(32) NOT NULL,
    body VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_task_activity_task_created ON task_activity (task_id, created_at);
