CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE spaces (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    owner_id UUID NOT NULL REFERENCES users (id),
    revision BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE space_members (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id),
    role VARCHAR(20) NOT NULL,
    CONSTRAINT uq_space_members UNIQUE (space_id, user_id)
);

CREATE INDEX idx_space_members_user ON space_members (user_id);

CREATE TABLE board_columns (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    position INT NOT NULL
);

CREATE INDEX idx_columns_space_position ON board_columns (space_id, position);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    space_id UUID NOT NULL REFERENCES spaces (id) ON DELETE CASCADE,
    column_id UUID NOT NULL REFERENCES board_columns (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    position INT NOT NULL,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tasks_column_position ON tasks (column_id, position);
