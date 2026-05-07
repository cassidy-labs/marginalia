package org.cassidylabs.marginalia.application.service;

import org.cassidylabs.marginalia.application.port.in.document.DocumentUseCase.*;
import org.cassidylabs.marginalia.application.port.out.DocumentPort;
import org.cassidylabs.marginalia.application.port.out.StoragePort;
import org.cassidylabs.marginalia.domain.document.Document;
import org.cassidylabs.marginalia.domain.document.DocumentStatus;
import org.cassidylabs.marginalia.global.exception.DocumentNotFoundException;
import org.cassidylabs.marginalia.global.exception.DocumentNotReadyException;
import org.cassidylabs.marginalia.global.exception.UnauthorizedException;
import org.cassidylabs.marginalia.global.exception.UploadNotCompleteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService")
class DocumentServiceTest {

    @Mock DocumentPort documentPort;
    @Mock StoragePort storagePort;

    @InjectMocks DocumentService documentService;

    private static final UUID USER_ID  = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();
    private static final UUID DOC_ID   = UUID.randomUUID();

    // ── prepareUpload ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("prepareUpload() — presigned URL 반환, R2 키 패턴 {userId}/{UUID}.pdf")
    void prepareUpload_returnsUploadUrlAndDocumentId() {
        Document saved = pendingDocument();
        given(storagePort.generateUploadUrl(any())).willReturn("https://r2.example.com/upload");
        given(documentPort.save(any())).willReturn(saved);

        UploadResult result = documentService.prepareUpload(new UploadCommand(USER_ID, "thesis.pdf"));

        assertThat(result.documentId()).isEqualTo(saved.getId());
        assertThat(result.uploadUrl()).isEqualTo("https://r2.example.com/upload");

        // R2 키가 {userId}/ 로 시작하는지 확인
        then(storagePort).should().generateUploadUrl(argThat(key -> key.startsWith(USER_ID.toString())));
    }

    // ── confirmUpload ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmUpload() — PENDING 문서를 READY로 전이")
    void confirmUpload_transitionsToReady() {
        Document doc = pendingDocument();
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(doc));
        given(storagePort.exists(doc.getR2Key())).willReturn(true);
        given(documentPort.save(any())).willReturn(doc);

        documentService.confirmUpload(new ConfirmCommand(DOC_ID, USER_ID, 2048L));

        assertThat(doc.isReady()).isTrue();
        assertThat(doc.getFileSize()).isEqualTo(2048L);
        then(documentPort).should().save(doc);
    }

    @Test
    @DisplayName("confirmUpload() — fileSize null이면 0L로 처리")
    void confirmUpload_nullFileSizeTreatedAsZero() {
        Document doc = pendingDocument();
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(doc));
        given(storagePort.exists(doc.getR2Key())).willReturn(true);
        given(documentPort.save(any())).willReturn(doc);

        assertThatCode(() -> documentService.confirmUpload(new ConfirmCommand(DOC_ID, USER_ID, null)))
                .doesNotThrowAnyException();
        assertThat(doc.getFileSize()).isEqualTo(0L);
    }

    @Test
    @DisplayName("confirmUpload() — 스토리지에 파일 없으면 UploadNotCompleteException")
    void confirmUpload_storageObjectMissingThrows() {
        Document doc = pendingDocument();
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(doc));
        given(storagePort.exists(doc.getR2Key())).willReturn(false);

        assertThatThrownBy(() -> documentService.confirmUpload(new ConfirmCommand(DOC_ID, USER_ID, 1024L)))
                .isInstanceOf(UploadNotCompleteException.class);

        then(documentPort).should(never()).save(any());
    }

    @Test
    @DisplayName("confirmUpload() — 다른 유저 문서 접근 시 UnauthorizedException")
    void confirmUpload_otherUserThrows() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(pendingDocument()));

        assertThatThrownBy(() -> documentService.confirmUpload(new ConfirmCommand(DOC_ID, OTHER_ID, 1024L)))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── getDocument ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDocument() — READY 문서면 presigned viewUrl 포함 반환")
    void getDocument_returnsViewUrl() {
        Document doc = readyDocument();
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(doc));
        given(storagePort.generateViewUrl(doc.getR2Key())).willReturn("https://r2.example.com/view");

        DocumentViewResult result = documentService.getDocument(DOC_ID, USER_ID);

        assertThat(result.viewUrl()).isEqualTo("https://r2.example.com/view");
        assertThat(result.id()).isEqualTo(doc.getId());
    }

    @Test
    @DisplayName("getDocument() — PENDING 문서면 DocumentNotReadyException")
    void getDocument_pendingThrows() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(pendingDocument()));

        assertThatThrownBy(() -> documentService.getDocument(DOC_ID, USER_ID))
                .isInstanceOf(DocumentNotReadyException.class);
    }

    @Test
    @DisplayName("getDocument() — 존재하지 않는 문서면 DocumentNotFoundException")
    void getDocument_notFoundThrows() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(DOC_ID, USER_ID))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("getDocument() — 다른 유저 문서 접근 시 UnauthorizedException")
    void getDocument_otherUserThrows() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(readyDocument()));

        assertThatThrownBy(() -> documentService.getDocument(DOC_ID, OTHER_ID))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── listDocuments ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("listDocuments() — READY 문서만 반환")
    void listDocuments_returnsOnlyReady() {
        Document ready   = readyDocument();
        Document pending = pendingDocument();
        given(documentPort.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of(ready, pending));

        List<DocumentResult> result = documentService.listDocuments(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(ready.getId());
    }

    @Test
    @DisplayName("listDocuments() — 문서 없으면 빈 목록")
    void listDocuments_emptyWhenNone() {
        given(documentPort.findByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(List.of());

        assertThat(documentService.listDocuments(USER_ID)).isEmpty();
    }

    // ── deleteDocument ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteDocument() — R2 파일과 DB 레코드 모두 삭제")
    void deleteDocument_deletesStorageAndDb() {
        Document doc = readyDocument();
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(doc));

        documentService.deleteDocument(DOC_ID, USER_ID);

        then(storagePort).should().delete(doc.getR2Key());
        then(documentPort).should().delete(doc);
    }

    @Test
    @DisplayName("deleteDocument() — 다른 유저 삭제 시도면 UnauthorizedException")
    void deleteDocument_otherUserThrows() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(readyDocument()));

        assertThatThrownBy(() -> documentService.deleteDocument(DOC_ID, OTHER_ID))
                .isInstanceOf(UnauthorizedException.class);

        then(storagePort).shouldHaveNoInteractions();
        then(documentPort).should(never()).delete(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Document pendingDocument() {
        return Document.builder()
                .id(DOC_ID)
                .userId(USER_ID)
                .fileName("thesis.pdf")
                .r2Key(USER_ID + "/thesis.pdf")
                .status(DocumentStatus.PENDING)
                .build();
    }

    private Document readyDocument() {
        Document doc = Document.builder()
                .id(DOC_ID)
                .userId(USER_ID)
                .fileName("thesis.pdf")
                .r2Key(USER_ID + "/thesis.pdf")
                .status(DocumentStatus.PENDING)
                .fileSize(1024L)
                .build();
        doc.confirm(1024L);
        return doc;
    }
}
