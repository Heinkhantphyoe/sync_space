ALTER TABLE tasks
    ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE tasks
    ADD CONSTRAINT tasks_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

CREATE TABLE task_labels (
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    label VARCHAR(20) NOT NULL,
    PRIMARY KEY (task_id, label),
    CONSTRAINT task_labels_label_check CHECK (label IN ('BUG', 'FEATURE', 'DESIGN'))
);
