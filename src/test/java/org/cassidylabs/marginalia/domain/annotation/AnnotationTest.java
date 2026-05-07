package org.cassidylabs.marginalia.domain.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Annotation 도메인")
class AnnotationTest {

    private static final UUID DOC_ID  = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_ID = UUID.randomUUID();

    private static final String RECTS    = "[{\"x1\":10,\"y1\":20,\"x2\":100,\"y2\":40}]";
    private static final String SELECTOR = "{\"type\":\"TextQuoteSelector\",\"exact\":\"존재는 본질에 앞선다\",\"prefix\":\"\",\"suffix\":\"\"}";

    // ── create() — pageNumber 검증(CodeRabbit) ─────────────────────────────────

    @Test
    @DisplayName("create() — 정상 케이스: 필드 저장 확인")
    void create_storesFields() {
        Annotation ann = create(1);

        assertThat(ann.getDocumentId()).isEqualTo(DOC_ID);
        assertThat(ann.getUserId()).isEqualTo(USER_ID);
        assertThat(ann.getPageNumber()).isEqualTo(1);
        assertThat(ann.getRects()).isEqualTo(RECTS);
        assertThat(ann.getQuoteSelector()).isEqualTo(SELECTOR);
        assertThat(ann.getTextSnapshot()).isEqualTo("존재는 본질에 앞선다");
        assertThat(ann.getType()).isEqualTo(AnnotationType.HIGHLIGHT);
        assertThat(ann.getColor()).isEqualTo(AnnotationColor.YELLOW);
    }

    @Test
    @DisplayName("create() — 메모 포함 저장")
    void create_storesMemo() {
        Annotation ann = createWithMemo("사르트르의 핵심 명제");

        assertThat(ann.getMemo()).isEqualTo("사르트르의 핵심 명제");
    }

    @Test
    @DisplayName("create() — 메모 null 허용")
    void create_acceptsNullMemo() {
        assertThatCode(() -> create(1)).doesNotThrowAnyException();
        assertThat(create(1).getMemo()).isNull();
    }

    @ParameterizedTest(name = "pageNumber={0}")
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("create() — pageNumber 1 미만이면 IllegalArgumentException")
    void create_rejectsInvalidPageNumber(int invalidPage) {
        assertThatThrownBy(() -> create(invalidPage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 이상");
    }

    @Test
    @DisplayName("create() — pageNumber 1은 허용")
    void create_acceptsPageNumberOne() {
        assertThatCode(() -> create(1)).doesNotThrowAnyException();
    }

    // ── updateMemo() ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateMemo() — 메모 교체")
    void updateMemo_replacesMemo() {
        Annotation ann = createWithMemo("initial");

        ann.updateMemo("updated");

        assertThat(ann.getMemo()).isEqualTo("updated");
    }

    @Test
    @DisplayName("updateMemo() — null로 메모 삭제 가능")
    void updateMemo_clearsMemoWithNull() {
        Annotation ann = createWithMemo("initial");

        ann.updateMemo(null);

        assertThat(ann.getMemo()).isNull();
    }

    // ── isOwnedBy() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("isOwnedBy() — 소유자 UUID면 true")
    void isOwnedBy_returnsTrueForOwner() {
        assertThat(create(1).isOwnedBy(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("isOwnedBy() — 다른 UUID면 false")
    void isOwnedBy_returnsFalseForOther() {
        assertThat(create(1).isOwnedBy(OTHER_ID)).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Annotation create(int pageNumber) {
        return Annotation.create(DOC_ID, USER_ID, pageNumber, RECTS, SELECTOR,
                "존재는 본질에 앞선다", AnnotationType.HIGHLIGHT, AnnotationColor.YELLOW, null);
    }

    private static Annotation createWithMemo(String memo) {
        return Annotation.create(DOC_ID, USER_ID, 1, RECTS, SELECTOR,
                "존재는 본질에 앞선다", AnnotationType.HIGHLIGHT, AnnotationColor.YELLOW, memo);
    }
}
