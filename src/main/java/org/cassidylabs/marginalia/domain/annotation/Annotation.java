package org.cassidylabs.marginalia.domain.annotation;

import jakarta.persistence.*;
import lombok.*;
import org.cassidylabs.marginalia.domain.BaseTimeEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "annotations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Annotation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    /**
     * PDF 좌표 공간 기준 rect 배열.
     * JSON 형식: [{x1, y1, x2, y2}, ...]
     * start_offset/end_offset 없이 rects + TextQuoteSelector 혼합 저장.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rects", columnDefinition = "jsonb", nullable = false)
    private String rects;

    /**
     * W3C TextQuoteSelector.
     * JSON 형식: {type, exact, prefix, suffix}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quote_selector", columnDefinition = "jsonb", nullable = false)
    private String quoteSelector;

    /** 선택된 원본 텍스트 — Skim 패널 표시 및 검색용 */
    @Column(name = "text_snapshot", nullable = false, columnDefinition = "TEXT")
    private String textSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AnnotationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", length = 20)
    private AnnotationColor color;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────

    public static Annotation create(
            UUID documentId,
            UUID userId,
            int pageNumber,
            String rects,
            String quoteSelector,
            String textSnapshot,
            AnnotationType type,
            AnnotationColor color
    ) {
        return Annotation.builder()
                .documentId(documentId)
                .userId(userId)
                .pageNumber(pageNumber)
                .rects(rects)
                .quoteSelector(quoteSelector)
                .textSnapshot(textSnapshot)
                .type(type)
                .color(color)
                .build();
    }

    // ── 도메인 메서드 ──────────────────────────────────────────────────────────

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }
}
