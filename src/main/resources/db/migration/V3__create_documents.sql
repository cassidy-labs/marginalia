CREATE TABLE documents (
    id         UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    file_name  VARCHAR(500) NOT NULL,
    r2_key     VARCHAR(500) NOT NULL,
    file_size  BIGINT,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_documents_status CHECK (status IN ('PENDING', 'READY'))
);

CREATE INDEX idx_documents_user ON documents (user_id, created_at DESC);
