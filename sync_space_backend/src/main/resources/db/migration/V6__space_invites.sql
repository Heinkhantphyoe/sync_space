CREATE TABLE space_invites (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    invited_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_space_invites UNIQUE (space_id, email)
);

CREATE INDEX idx_space_invites_email ON space_invites (email);
