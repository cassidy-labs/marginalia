package org.cassidylabs.marginalia.application.port.out;

public interface StoragePort {

    /**
     * 브라우저 → 스토리지 직접 업로드용 presigned PUT URL.
     *
     * @param key R2/로컬 오브젝트 키
     * @return 업로드 URL (유효기간 15분)
     */
    String generateUploadUrl(String key);

    /**
     * PDF 조회용 presigned GET URL.
     *
     * @param key R2/로컬 오브젝트 키
     * @return 조회 URL (유효기간 1시간)
     */
    String generateViewUrl(String key);

    /**
     * 오브젝트가 실제로 존재하는지 확인.
     */
    boolean exists(String key);

    void delete(String key);
}
