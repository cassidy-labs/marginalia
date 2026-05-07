CREATE TABLE annotations (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    document_id    UUID        NOT NULL,
    user_id        UUID        NOT NULL,
    page_number    INTEGER     NOT NULL,
    rects          JSONB       NOT NULL,
    quote_selector JSONB       NOT NULL,
    text_snapshot  TEXT        NOT NULL,
    type           VARCHAR(20) NOT NULL,
    color          VARCHAR(20),
    memo           TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_annotations PRIMARY KEY (id),
    CONSTRAINT fk_annotations_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_annotations_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT chk_annotations_type  CHECK (type  IN ('HIGHLIGHT', 'UNDERLINE', 'MEMO')),
    CONSTRAINT chk_annotations_color CHECK (color IN ('YELLOW', 'BLUE', 'GREEN', 'PINK'))
);

-- 페이지 단위 lazy load 핵심 인덱스
CREATE INDEX idx_annotations_doc_page ON annotations (document_id, page_number);
-- 사용자별 최신 목록
CREATE INDEX idx_annotations_user ON annotations (user_id, created_at DESC);
