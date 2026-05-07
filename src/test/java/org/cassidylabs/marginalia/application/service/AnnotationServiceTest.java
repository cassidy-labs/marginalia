package org.cassidylabs.marginalia.application.service;

import org.cassidylabs.marginalia.application.port.in.annotation.AnnotationUseCase.*;
import org.cassidylabs.marginalia.application.port.out.AnnotationPort;
import org.cassidylabs.marginalia.application.port.out.DocumentPort;
import org.cassidylabs.marginalia.domain.annotation.Annotation;
import org.cassidylabs.marginalia.domain.annotation.AnnotationColor;
import org.cassidylabs.marginalia.domain.annotation.AnnotationType;
import org.cassidylabs.marginalia.domain.document.Document;
import org.cassidylabs.marginalia.domain.document.DocumentStatus;
import org.cassidylabs.marginalia.global.exception.AnnotationNotFoundException;
import org.cassidylabs.marginalia.global.exception.DocumentNotFoundException;
import org.cassidylabs.marginalia.global.exception.UnauthorizedException;
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
@DisplayName("AnnotationService")
class AnnotationServiceTest {

    @Mock AnnotationPort annotationPort;
    @Mock DocumentPort documentPort;

    @InjectMocks AnnotationService annotationService;

    private static final UUID DOC_ID  = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();
    private static final UUID ANN_ID  = UUID.randomUUID();

    private static final String RECTS    = "[{\"x1\":10,\"y1\":20,\"x2\":100,\"y2\":40}]";
    private static final String SELECTOR = "{\"type\":\"TextQuoteSelector\",\"exact\":\"test\",\"prefix\":\"\",\"suffix\":\"\"}";

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save() — 정상 케이스: 어노테이션 저장 후 결과 반환")
    void save_success() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(readyDocument(USER_ID)));
        Annotation saved = buildAnnotation(USER_ID, null);
        given(annotationPort.save(any())).willReturn(saved);

        SaveCommand cmd = saveCommand(null);
        AnnotationResult result = annotationService.save(cmd);

        assertThat(result.documentId()).isEqualTo(DOC_ID);
        assertThat(result.pageNumber()).isEqualTo(3);
        assertThat(result.type()).isEqualTo(AnnotationType.HIGHLIGHT);
        assertThat(result.color()).isEqualTo(AnnotationColor.YELLOW);
        then(annotationPort).should().save(any());
    }

    @Test
    @DisplayName("save() — 메모 포함 저장")
    void save_withMemo() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(readyDocument(USER_ID)));
        Annotation saved = buildAnnotation(USER_ID, "사르트르");
        given(annotationPort.save(any())).willReturn(saved);

        AnnotationResult result = annotationService.save(saveCommand("사르트르"));

        assertThat(result.memo()).isEqualTo("사르트르");
    }

    @Test
    @DisplayName("save() — 문서 없으면 DocumentNotFoundException")
    void save_documentNotFound() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> annotationService.save(saveCommand(null)))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    @DisplayName("save() — 다른 유저 문서에 저장 시도면 UnauthorizedException (403, 문서 존재 누설 방지)")
    void save_otherUserDocumentThrows() {
        given(documentPort.findById(DOC_ID)).willReturn(Optional.of(readyDocument(OTHER_ID)));

        assertThatThrownBy(() -> annotationService.save(saveCommand(null)))
                .isInstanceOf(UnauthorizedException.class);

        then(annotationPort).shouldHaveNoInteractions();
    }

    // ── getByPage ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByPage() — 페이지 어노테이션 목록 반환")
    void getByPage_returnsAnnotations() {
        List<Annotation> anns = List.of(buildAnnotation(USER_ID, null), buildAnnotation(USER_ID, "memo2"));
        given(annotationPort.findByDocumentIdAndPageNumberAndUserId(DOC_ID, 3, USER_ID))
                .willReturn(anns);

        List<AnnotationResult> result = annotationService.getByPage(DOC_ID, 3, USER_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getByPage() — 어노테이션 없으면 빈 목록")
    void getByPage_emptyPage() {
        given(annotationPort.findByDocumentIdAndPageNumberAndUserId(DOC_ID, 99, USER_ID))
                .willReturn(List.of());

        assertThat(annotationService.getByPage(DOC_ID, 99, USER_ID)).isEmpty();
    }

    // ── updateMemo ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateMemo() — 메모 교체 후 저장")
    void updateMemo_success() {
        Annotation ann = buildAnnotation(USER_ID, "before");
        given(annotationPort.findById(ANN_ID)).willReturn(Optional.of(ann));
        given(annotationPort.save(ann)).willReturn(ann);

        AnnotationResult result = annotationService.updateMemo(ANN_ID, USER_ID, "after");

        assertThat(result.memo()).isEqualTo("after");
        then(annotationPort).should().save(ann);
    }

    @Test
    @DisplayName("updateMemo() — 존재하지 않는 ID면 AnnotationNotFoundException")
    void updateMemo_notFound() {
        given(annotationPort.findById(ANN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> annotationService.updateMemo(ANN_ID, USER_ID, "memo"))
                .isInstanceOf(AnnotationNotFoundException.class);
    }

    @Test
    @DisplayName("updateMemo() — 다른 유저 어노테이션 수정 시 UnauthorizedException")
    void updateMemo_otherUserThrows() {
        given(annotationPort.findById(ANN_ID)).willReturn(Optional.of(buildAnnotation(USER_ID, null)));

        assertThatThrownBy(() -> annotationService.updateMemo(ANN_ID, OTHER_ID, "memo"))
                .isInstanceOf(UnauthorizedException.class);

        then(annotationPort).should(never()).save(any());
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() — 소유자가 삭제하면 delete 호출")
    void delete_success() {
        Annotation ann = buildAnnotation(USER_ID, null);
        given(annotationPort.findById(ANN_ID)).willReturn(Optional.of(ann));

        annotationService.delete(ANN_ID, USER_ID);

        then(annotationPort).should().delete(ann);
    }

    @Test
    @DisplayName("delete() — 존재하지 않는 ID면 AnnotationNotFoundException")
    void delete_notFound() {
        given(annotationPort.findById(ANN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> annotationService.delete(ANN_ID, USER_ID))
                .isInstanceOf(AnnotationNotFoundException.class);
    }

    @Test
    @DisplayName("delete() — 다른 유저가 삭제 시도면 UnauthorizedException")
    void delete_otherUserThrows() {
        given(annotationPort.findById(ANN_ID)).willReturn(Optional.of(buildAnnotation(USER_ID, null)));

        assertThatThrownBy(() -> annotationService.delete(ANN_ID, OTHER_ID))
                .isInstanceOf(UnauthorizedException.class);

        then(annotationPort).should(never()).delete(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SaveCommand saveCommand(String memo) {
        return new SaveCommand(
                DOC_ID, USER_ID, 3, RECTS, SELECTOR,
                "test text", AnnotationType.HIGHLIGHT, AnnotationColor.YELLOW, memo);
    }

    private Annotation buildAnnotation(UUID userId, String memo) {
        return Annotation.builder()
                .id(ANN_ID)
                .documentId(DOC_ID)
                .userId(userId)
                .pageNumber(3)
                .rects(RECTS)
                .quoteSelector(SELECTOR)
                .textSnapshot("test text")
                .type(AnnotationType.HIGHLIGHT)
                .color(AnnotationColor.YELLOW)
                .memo(memo)
                .build();
    }

    private Document readyDocument(UUID ownerId) {
        Document doc = Document.builder()
                .id(DOC_ID)
                .userId(ownerId)
                .fileName("thesis.pdf")
                .r2Key(ownerId + "/thesis.pdf")
                .status(DocumentStatus.PENDING)
                .build();
        doc.confirm(1024L);
        return doc;
    }
}
