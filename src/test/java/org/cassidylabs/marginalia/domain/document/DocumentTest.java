package org.cassidylabs.marginalia.domain.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Document 도메인")
class DocumentTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    private Document document;

    @BeforeEach
    void setUp() {
        document = Document.create(USER_ID, "thesis.pdf", "user-id/doc-id.pdf");
    }

    // ── create() ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create() — 초기 상태는 PENDING")
    void create_initialStatusIsPending() {
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.PENDING);
        assertThat(document.isReady()).isFalse();
    }

    @Test
    @DisplayName("create() — 파일명·소유자·R2 키 저장")
    void create_storesMetadata() {
        assertThat(document.getFileName()).isEqualTo("thesis.pdf");
        assertThat(document.getUserId()).isEqualTo(USER_ID);
        assertThat(document.getR2Key()).isEqualTo("user-id/doc-id.pdf");
    }

    // ── confirm() — 입력값 검증이 상태 검증보다 먼저(CodeRabbit) ────────────────

    @Test
    @DisplayName("confirm() — 유효한 파일 크기면 READY로 전이")
    void confirm_transitionsToReady() {
        document.confirm(1024L);

        assertThat(document.isReady()).isTrue();
        assertThat(document.getFileSize()).isEqualTo(1024L);
    }

    @Test
    @DisplayName("confirm() — 파일 크기 0은 허용")
    void confirm_acceptsZeroFileSize() {
        assertThatCode(() -> document.confirm(0L)).doesNotThrowAnyException();
        assertThat(document.isReady()).isTrue();
    }

    @Test
    @DisplayName("confirm() — 음수 파일 크기는 IllegalArgumentException (입력 검증 우선)")
    void confirm_rejectsNegativeFileSize() {
        assertThatThrownBy(() -> document.confirm(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상");
    }

    @Test
    @DisplayName("confirm() — 음수 크기 전달 시 상태가 PENDING이 아니어도 IllegalArgumentException 먼저")
    void confirm_inputValidationBeforeStateCheck() {
        // 이미 READY 상태로 만든 후
        document.confirm(100L);

        // 이미 READY인 문서에 음수 크기 전달 → 상태가 아닌 입력 오류가 먼저 나와야 함
        assertThatThrownBy(() -> document.confirm(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상");
    }

    @Test
    @DisplayName("confirm() — 이미 READY인 문서에 유효한 크기 전달 시 IllegalStateException")
    void confirm_throwsWhenAlreadyReady() {
        document.confirm(100L);

        assertThatThrownBy(() -> document.confirm(200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 확인된 문서");
    }

    // ── isOwnedBy() ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("isOwnedBy() — 소유자 UUID면 true")
    void isOwnedBy_returnsTrueForOwner() {
        assertThat(document.isOwnedBy(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("isOwnedBy() — 다른 UUID면 false")
    void isOwnedBy_returnsFalseForOther() {
        assertThat(document.isOwnedBy(OTHER_USER_ID)).isFalse();
    }
}
