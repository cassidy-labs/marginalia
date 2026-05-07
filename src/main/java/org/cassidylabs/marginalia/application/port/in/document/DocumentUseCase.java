package org.cassidylabs.marginalia.application.port.in.document;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentUseCase {

    record UploadCommand(UUID userId, String fileName) {}

    record UploadResult(UUID documentId, String uploadUrl) {}

    record DocumentResult(UUID id, String fileName, Long fileSize, Instant createdAt) {}

    record DocumentViewResult(UUID id, String fileName, String viewUrl, Long fileSize, Instant createdAt) {}

    record ConfirmCommand(UUID documentId, UUID userId, Long fileSize) {}

    /** presigned PUT URL 발급 + PENDING Document 생성 */
    UploadResult prepareUpload(UploadCommand command);

    /** 업로드 완료 확인 — PENDING → READY */
    void confirmUpload(ConfirmCommand command);

    /** presigned GET URL 포함 조회 */
    DocumentViewResult getDocument(UUID documentId, UUID userId);

    List<DocumentResult> listDocuments(UUID userId);

    /** R2 파일 + DB 레코드 함께 삭제 */
    void deleteDocument(UUID documentId, UUID userId);
}
