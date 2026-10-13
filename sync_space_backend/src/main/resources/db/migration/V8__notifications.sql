CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    space_id UUID NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users (id),
    kind VARCHAR(20) NOT NULL,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CONSTRAINT notifications_kind_check CHECK (kind IN ('ASSIGNED', 'COMMENT'))
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
