package org.cassidylabs.marginalia.domain.document;

public enum DocumentStatus {

    /** presigned PUT URL 발급됨, 업로드 미완료 */
    PENDING,

    /** 업로드 완료, 조회 가능 */
    READY
}
