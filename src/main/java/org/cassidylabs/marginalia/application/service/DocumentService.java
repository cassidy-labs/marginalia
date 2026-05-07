package org.cassidylabs.marginalia.application.service;

import lombok.RequiredArgsConstructor;
import org.cassidylabs.marginalia.application.port.in.document.DocumentUseCase;
import org.cassidylabs.marginalia.application.port.out.DocumentPort;
import org.cassidylabs.marginalia.application.port.out.StoragePort;
import org.cassidylabs.marginalia.domain.document.Document;
import org.cassidylabs.marginalia.global.exception.DocumentNotFoundException;
import org.cassidylabs.marginalia.global.exception.DocumentNotReadyException;
import org.cassidylabs.marginalia.global.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService implements DocumentUseCase {

    private final DocumentPort documentPort;
    private final StoragePort storagePort;

    @Override
    public UploadResult prepareUpload(UploadCommand command) {
        // R2 키 패턴: {userId}/{UUID}.pdf
        String r2Key = command.userId() + "/" + UUID.randomUUID() + ".pdf";
        String uploadUrl = storagePort.generateUploadUrl(r2Key);

        Document document = documentPort.save(Document.create(command.userId(), command.fileName(), r2Key));
        return new UploadResult(document.getId(), uploadUrl);
    }

    @Override
    public void confirmUpload(ConfirmCommand command) {
        Document document = getOwnedDocument(command.documentId(), command.userId());
        document.confirm(command.fileSize() != null ? command.fileSize() : 0L);
        documentPort.save(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentViewResult getDocument(UUID documentId, UUID userId) {
        Document document = getOwnedDocument(documentId, userId);
        if (!document.isReady()) {
            throw new DocumentNotReadyException(documentId);
        }
        String viewUrl = storagePort.generateViewUrl(document.getR2Key());
        return toViewResult(document, viewUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResult> listDocuments(UUID userId) {
        return documentPort.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(Document::isReady)
                .map(this::toResult)
                .toList();
    }

    @Override
    public void deleteDocument(UUID documentId, UUID userId) {
        Document document = getOwnedDocument(documentId, userId);
        storagePort.delete(document.getR2Key());
        documentPort.delete(document);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private Document getOwnedDocument(UUID documentId, UUID userId) {
        Document document = documentPort.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (!document.isOwnedBy(userId)) {
            throw new UnauthorizedException("문서에 접근 권한이 없습니다.");
        }
        return document;
    }

    private DocumentResult toResult(Document d) {
        return new DocumentResult(d.getId(), d.getFileName(), d.getFileSize(), d.getCreatedAt());
    }

    private DocumentViewResult toViewResult(Document d, String viewUrl) {
        return new DocumentViewResult(d.getId(), d.getFileName(), viewUrl, d.getFileSize(), d.getCreatedAt());
    }
}
