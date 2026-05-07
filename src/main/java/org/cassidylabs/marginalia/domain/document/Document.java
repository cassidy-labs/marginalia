package org.cassidylabs.marginalia.domain.document;

import jakarta.persistence.*;
import lombok.*;
import org.cassidylabs.marginalia.domain.BaseTimeEntity;

import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Document extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /** Cloudflare R2 오브젝트 키 (패턴: {userId}/{documentId}.pdf) */
    @Column(name = "r2_key", nullable = false, length = 500)
    private String r2Key;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DocumentStatus status;

    // ── 팩토리 메서드 ──────────────────────────────────────────────────────────

    public static Document create(UUID userId, String fileName, String r2Key) {
        return Document.builder()
                .userId(userId)
                .fileName(fileName)
                .r2Key(r2Key)
                .status(DocumentStatus.PENDING)
                .build();
    }

    // ── 도메인 메서드 ──────────────────────────────────────────────────────────

    /**
     * 업로드 완료 확인.
     * PENDING → READY 상태 전이.
     */
    public void confirm(long fileSize) {
        if (this.status != DocumentStatus.PENDING) {
            throw new IllegalStateException("이미 확인된 문서입니다: " + id);
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("파일 크기는 0 이상이어야 합니다: " + fileSize);
        }
        this.status = DocumentStatus.READY;
        this.fileSize = fileSize;
    }

    public boolean isReady() {
        return status == DocumentStatus.READY;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }
}
